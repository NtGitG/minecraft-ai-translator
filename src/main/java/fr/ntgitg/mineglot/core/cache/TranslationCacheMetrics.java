package fr.ntgitg.mineglot.core.cache;

import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.atomic.AtomicLong;

final class TranslationCacheMetrics {

    private final TranslationMemoryCache memoryCache;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();

    TranslationCacheMetrics(TranslationMemoryCache memoryCache) {
        this.memoryCache = memoryCache;
    }

    void recordHit() {
        hitCount.incrementAndGet();
    }

    void recordMiss() {
        missCount.incrementAndGet();
    }

    long getCacheSize() {
        return memoryCache.getCacheSize();
    }

    double getHitRate() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long requests = hits + misses;
        if (requests == 0) {
            return 1.0D;
        }
        return (double) hits / requests;
    }

    long getHitCount() {
        return hitCount.get();
    }

    long getMissCount() {
        return missCount.get();
    }

    long getMemoryBytes() {
        try {
            return memoryCache.getMemoryBytes();
        } catch (Exception e) {
            ModLogger.error("Erreur lors du calcul de la memoire du cache", e);
            long estimatedSize = getCacheSize() * 100;
            ModLogger.warn("Estimation memoire utilisee: {} bytes", estimatedSize);
            return estimatedSize;
        }
    }
}
