package fr.ntgitg.mineglot.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.storage.TranslationStorage;
import fr.ntgitg.mineglot.core.translation.TranslationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class TranslationCache {

    private static final int MAX_LOG_LENGTH = 80;
    private static final long DB_MISS_TTL_MS = 2_000L;

    private final ConfigurationManager configManager;
    private final TranslationMemoryCache memoryCache;
    private final TranslationCacheState state;
    private final TranslationCacheStore store;
    private final TranslationCacheMetrics metrics;
    private final AtomicLong globalInvalidationVersion = new AtomicLong();
    private final AtomicLong keyInvalidationSequence = new AtomicLong();
    private final ConcurrentMap<String, Long> keyInvalidationVersions = new ConcurrentHashMap<>();
    private final Cache<String, Boolean> recentDbMisses = CacheBuilder.newBuilder()
            .expireAfterWrite(DB_MISS_TTL_MS, TimeUnit.MILLISECONDS)
            .maximumSize(4096)
            .build();

    public static TranslationCache getInstance() {
        return SingletonManager.getInstance(TranslationCache.class, TranslationCache::new);
    }

    private TranslationCache() {
        this.configManager = ConfigurationManager.getInstance();
        this.store = new TranslationCacheStore();
        this.state = new TranslationCacheState();

        long maxBytes;
        try {
            maxBytes = configManager.getMaxCacheMemory() * 1024L * 1024L;
        } catch (Exception e) {
            ModLogger.warn("ConfigurationManager non disponible, valeur par defaut (100MB)");
            maxBytes = 100L * 1024L * 1024L;
        }

        this.memoryCache = new TranslationMemoryCache(maxBytes);
        this.metrics = new TranslationCacheMetrics(memoryCache);
    }

    /**
     * Resultat d'une recherche dans le cache unifie : la valeur (ou null) ET la cle
     * deja calculee. Permet a l'appelant de reutiliser la cle sans la re-hasher.
     */
    public static final class UnifiedLookup {
        private final String value;
        private final String key;

        private UnifiedLookup(String value, String key) {
            this.value = value;
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public String getKey() {
            return key;
        }
    }

    public UnifiedLookup lookupUnified(String text, String targetLang) {
        String key = TranslationStorage.generateUnifiedCacheKey(text, targetLang);

        String cached = memoryCache.getTranslation(key);
        if (cached != null) {
            ModLogger.debug("[CACHE] Hit Guava pour: '{}'", text);
            return new UnifiedLookup(cached, key);
        }

        if (hasRecentDbMiss(key)) {
            return new UnifiedLookup(null, key);
        }

        try {
            TranslationStorage.RawCacheResult dbResult = store.getRawCacheValue(text, targetLang);
            String dbVal = dbResult.getValue();
            if (dbVal == null) {
                recordDbMiss(key);
                return new UnifiedLookup(null, key);
            }

            clearDbMiss(key);
            if (dbResult.isLegacy()) {
                ModLogger.debug("[CACHE] Hit RocksDB legacy pour: '{}'", text);
            } else {
                ModLogger.debug("[CACHE] Hit RocksDB pour: '{}'", text);
            }

            memoryCache.putTranslation(key, dbVal);

            if (dbResult.isLegacy()) {
                store.migrateLegacyAsync(text, targetLang, dbVal, key);
            }

            return new UnifiedLookup(dbVal, key);
        } catch (Exception e) {
            ModLogger.error("Lecture RocksDB echouee", e);
            return new UnifiedLookup(null, key);
        }
    }

    public String getCachedTranslationByKey(String contextualKey) {
        String cached = memoryCache.getTranslation(contextualKey);
        if (cached != null) {
            ModLogger.debug("[CACHE] Hit Guava contextuel");
            return cached;
        }

        if (hasRecentDbMiss(contextualKey)) {
            return null;
        }

        try {
            TranslationStorage.RawCacheResult dbResult = store.getRawCacheValueByKey(contextualKey);
            String dbVal = dbResult.getValue();
            if (dbVal == null) {
                recordDbMiss(contextualKey);
                return null;
            }

            clearDbMiss(contextualKey);
            ModLogger.debug("[CACHE] Hit RocksDB contextuel");
            memoryCache.putTranslation(contextualKey, dbVal);

            return dbVal;
        } catch (Exception e) {
            ModLogger.error("Lecture RocksDB contextuelle echouee", e);
            return null;
        }
    }

    public CompletableFuture<Void> cacheTranslationAsyncByKey(String key, String translation) {
        if (isBlank(key) || translation == null) {
            return CompletableFuture.completedFuture(null);
        }

        WriteGuard writeGuard = createWriteGuard(key);
        memoryCache.putTranslation(key, translation);
        clearDbMiss(key);
        return store.writeRawAsyncByKey(key, translation, writeGuard::isCurrent);
    }

    public CompletableFuture<Void> cacheTranslation(String text, String targetLang, String translation) {
        return cacheTranslationByKey(TranslationStorage.generateUnifiedCacheKey(text, targetLang),
                translation);
    }

    public CompletableFuture<Void> cacheTranslationByKey(String key, String translation) {
        CompletableFuture<Void> writeFuture = cacheTranslationAsyncByKey(key, translation);
        if (isBlank(key) || translation == null) {
            return writeFuture;
        }

        String logVal = translation.length() > MAX_LOG_LENGTH
                ? translation.substring(0, MAX_LOG_LENGTH) + "..."
                : translation;

        ModLogger.debug("[CACHE] Entree ajoutee - Cle: '{}' | Valeur: {}", key, logVal);
        return writeFuture;
    }

    public String getCacheKey(String text, String sourceLang, String targetLang, String modelId) {
        return TranslationStorage.generateContextualCacheKey(text, sourceLang, targetLang, modelId);
    }

    public void clearCache() {
        markAllCacheWritesInvalidated();
        clearMemoryCache();
        try {
            store.clearCacheStore();
            ModLogger.info("Cache complet vide: Guava + RocksDB + etat");
        } catch (Exception e) {
            ModLogger.error("Erreur nettoyage RocksDB, cache memoire deja vide", e);
            ModLogger.warn("Le cache fonctionne mais des donnees persistent en base");
        }
    }

    public void clearMemoryCache() {
        memoryCache.clearAll();
        recentDbMisses.invalidateAll();
        state.resetTrsClearState();
        TranslationService.getInstance().resetTranslationState();
        ModLogger.info("Cache memoire et etat de traduction reinitialises (RocksDB conservee)");
    }

    public long getCacheSize() {
        return metrics.getCacheSize();
    }

    public void recordCacheHit() {
        metrics.recordHit();
    }

    public void recordCacheMiss() {
        metrics.recordMiss();
    }

    public double getHitRate() {
        return metrics.getHitRate();
    }

    public long getHitCount() {
        return metrics.getHitCount();
    }

    public long getMissCount() {
        return metrics.getMissCount();
    }

    public long getMemoryBytes() {
        return metrics.getMemoryBytes();
    }

    public boolean hasRecentTranslation() {
        return state.hasRecentTranslation();
    }

    public boolean isTrsClearCommandUsed() {
        return state.isTrsClearCommandUsed();
    }

    public void setTrsClearCommandUsed(boolean used) {
        state.setTrsClearCommandUsed(used);
    }

    public void setLastTranslatedText(String text) {
        state.setLastTranslatedText(text);
    }

    public void setLastTargetLanguage(String targetLang) {
        state.setLastTargetLanguage(targetLang);
    }

    public void setLastCacheKey(String cacheKey) {
        state.setLastCacheKey(cacheKey);
    }

    public void setLastTranslationTimestamp(long timestamp) {
        state.setLastTranslationTimestamp(timestamp);
    }

    public long getLastTranslationTimestamp() {
        return state.getLastTranslationTimestamp();
    }

    public String getLastTranslatedText() {
        return state.getLastTranslatedText();
    }

    public String getLastTargetLanguage() {
        return state.getLastTargetLanguage();
    }

    public String getLastCacheKey() {
        return state.getLastCacheKey();
    }

    public boolean clearSpecificTranslation(String text, String targetLang) {
        try {
            String key = TranslationStorage.generateUnifiedCacheKey(text, targetLang);
            invalidateCachedKey(key);

            try {
                store.deleteRawCacheValue(text, targetLang);
                ModLogger.debug("Traduction supprimee de RocksDB: {}", key);
            } catch (Exception e) {
                ModLogger.warn("Erreur suppression RocksDB pour la cle: {}", key, e);
            }

            return true;
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la suppression de la traduction", e);
            return false;
        }
    }

    public boolean clearLastTranslation() {
        String cacheKey = getLastCacheKey();
        String lastText = getLastTranslatedText();
        String lastTargetLanguage = getLastTargetLanguage();

        try {
            if (cacheKey != null && !cacheKey.trim().isEmpty()) {
                invalidateCachedKey(cacheKey);
                store.deleteRawCacheValueByKey(cacheKey);
            }

            if (lastText != null && lastTargetLanguage != null) {
                String unifiedKey = TranslationStorage.generateUnifiedCacheKey(lastText,
                        lastTargetLanguage);
                invalidateCachedKey(unifiedKey);
                store.deleteRawCacheValue(lastText, lastTargetLanguage);
            }

            ModLogger.debug("Derniere traduction supprimee du cache: {}", cacheKey);
            return true;
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la suppression de la derniere traduction", e);
            return false;
        }
    }

    private boolean hasRecentDbMiss(String key) {
        if (key == null) {
            return false;
        }
        return recentDbMisses.getIfPresent(key) != null;
    }

    private void recordDbMiss(String key) {
        if (key != null && !key.trim().isEmpty()) {
            recentDbMisses.put(key, Boolean.TRUE);
        }
    }

    private void clearDbMiss(String key) {
        if (key != null) {
            recentDbMisses.invalidate(key);
        }
    }

    private void invalidateCachedKey(String key) {
        if (isBlank(key)) {
            return;
        }
        markCacheKeyInvalidated(key);
        memoryCache.removeTranslation(key);
        clearDbMiss(key);
    }

    private void markCacheKeyInvalidated(String key) {
        if (!isBlank(key)) {
            keyInvalidationVersions.put(key, keyInvalidationSequence.incrementAndGet());
        }
    }

    private void markAllCacheWritesInvalidated() {
        globalInvalidationVersion.incrementAndGet();
        keyInvalidationVersions.clear();
    }

    private WriteGuard createWriteGuard(String key) {
        long globalVersion = globalInvalidationVersion.get();
        long keyVersion = getKeyInvalidationVersion(key);
        return () -> globalVersion == globalInvalidationVersion.get()
                && keyVersion == getKeyInvalidationVersion(key);
    }

    private long getKeyInvalidationVersion(String key) {
        Long version = keyInvalidationVersions.get(key);
        return version != null ? version : 0L;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private interface WriteGuard {
        boolean isCurrent();
    }
}
