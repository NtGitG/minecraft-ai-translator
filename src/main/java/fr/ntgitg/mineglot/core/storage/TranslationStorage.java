package fr.ntgitg.mineglot.core.storage;

import com.google.gson.Gson;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import org.rocksdb.WriteBatch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class TranslationStorage {

    private static final Gson GSON = new Gson();
    private static final String SEPARATOR = "_";
    private static final String DEFAULT_LANG = "en";
    private static final String DEFAULT_SOURCE_LANG = "auto";
    private static final String DEFAULT_MODEL = "default";
    private static final String CACHE_KEY_VERSION = "v2";
    private static final String RAW_CACHE_FORMAT = "mineglot_raw_v1";
    private static final String METADATA_PREFIX = "__mineglot_meta_";
    private static final String LAST_CLEANUP_KEY = METADATA_PREFIX + "last_cleanup_ms";
    private static final int CLEANUP_DELETE_BATCH_SIZE = 500;
    private static final long DAY_MS = 86_400_000L;

    public static class TranslationEntry {
        public String originalText;
        public String correctedText;
        public String translation;
        public String targetLang;
        public long timestamp;

        public TranslationEntry(String originalText, String correctedText, String translation,
                                String targetLang) {
            this(originalText, correctedText, translation, targetLang, System.currentTimeMillis());
        }

        public TranslationEntry(String originalText, String correctedText, String translation,
                                String targetLang, long timestamp) {
            this.originalText = originalText;
            this.correctedText = correctedText;
            this.translation = translation;
            this.targetLang = targetLang;
            this.timestamp = timestamp;
        }
    }

    public static final class RawCacheResult {
        private final String value;
        private final boolean legacy;

        private RawCacheResult(String value, boolean legacy) {
            this.value = value;
            this.legacy = legacy;
        }

        public String getValue() {
            return value;
        }

        public boolean isLegacy() {
            return legacy;
        }
    }

    private static final class RawCachePayload {
        private String format;
        private String translation;
        private long timestamp;

        private RawCachePayload(String translation, long timestamp) {
            this.format = RAW_CACHE_FORMAT;
            this.translation = translation;
            this.timestamp = timestamp;
        }
    }

    public static String generateUnifiedCacheKey(String text, String targetLang) {
        String safeText = normalizeText(text);
        String safeLang = normalizeLang(targetLang);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(safeText.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return hash + SEPARATOR + safeLang;
        } catch (Exception e) {
            ModLogger.error("Erreur SHA-256 dans generateUnifiedCacheKey, fallback securise", e);
            return generateCacheKey(safeText, safeLang);
        }
    }

    public static String generateContextualCacheKey(String text, String sourceLang,
                                                    String targetLang, String modelId) {
        String safeText = normalizeText(text);
        String safeSource = normalizeSourceLang(sourceLang);
        String safeTarget = normalizeLang(targetLang);
        String safeModel = normalizeModel(modelId);

        String keyMaterial = CACHE_KEY_VERSION
                + "\nsource=" + safeSource
                + "\ntarget=" + safeTarget
                + "\nmodel=" + safeModel
                + "\ntext=" + safeText;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return CACHE_KEY_VERSION + SEPARATOR + hash + SEPARATOR + safeTarget;
        } catch (Exception e) {
            ModLogger.error("Erreur SHA-256 dans generateContextualCacheKey, fallback securise", e);
            return generateCacheKey(keyMaterial, safeTarget);
        }
    }

    public static String generateCacheKey(String text, String lang) {
        String safeText = normalizeText(text);
        String safeLang = normalizeLang(lang);
        return safeText.toLowerCase(Locale.ROOT) + SEPARATOR + safeLang;
    }

    public static RawCacheResult getRawCacheValue(String text, String targetLang) {
        String key = generateUnifiedCacheKey(text, targetLang);
        String value = DatabaseOperations.get(key);
        if (value != null) {
            return new RawCacheResult(decodeRawCacheValue(value), false);
        }

        String legacyKey = generateCacheKey(text, targetLang);
        String legacyValue = DatabaseOperations.get(legacyKey);
        if (legacyValue != null) {
            return new RawCacheResult(decodeRawCacheValue(legacyValue), true);
        }

        return new RawCacheResult(null, false);
    }

    public static RawCacheResult getRawCacheValueByKey(String key) {
        String value = DatabaseOperations.get(key);
        return new RawCacheResult(decodeRawCacheValue(value), false);
    }

    public static void putRawCacheValue(String text, String targetLang, String translation) {
        String key = generateUnifiedCacheKey(text, targetLang);
        putRawCacheValueByKey(key, translation);
    }

    public static void putRawCacheValueByKey(String key, String translation) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        DatabaseOperations.put(key, encodeRawCacheValue(translation));
    }

    public static void deleteRawCacheValue(String text, String targetLang) {
        String key = generateUnifiedCacheKey(text, targetLang);
        deleteRawCacheValueByKey(key);

        String legacyKey = generateCacheKey(text, targetLang);
        if (!legacyKey.equals(key)) {
            deleteRawCacheValueByKey(legacyKey);
        }
    }

    public static void deleteRawCacheValueByKey(String key) {
        if (key != null && !key.trim().isEmpty()) {
            DatabaseOperations.delete(key);
        }
    }

    public static void clearCacheStore() {
        DatabaseOperations.clear();
    }

    public static boolean cleanupOldEntriesIfDue(long maxAgeMillis, long minIntervalMillis) {
        return cleanupOldEntriesIfDue(maxAgeMillis, minIntervalMillis, () -> false);
    }

    public static boolean cleanupOldEntriesIfDue(long maxAgeMillis, long minIntervalMillis,
                                                 BooleanSupplier shouldStop) {
        if (maxAgeMillis <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long lastCleanup = getLastCleanupTimestamp();
        if (lastCleanup > 0 && minIntervalMillis > 0 && now - lastCleanup < minIntervalMillis) {
            ModLogger.debug("Nettoyage des traductions ignore: dernier nettoyage recent");
            return false;
        }

        CleanupResult result = cleanupOldEntriesInternal(maxAgeMillis, shouldStop);
        if (result.completed) {
            setLastCleanupTimestamp(now);
        }
        return result.completed;
    }

    public static int cleanupOldEntries(long maxAgeMillis) {
        return cleanupOldEntries(maxAgeMillis, () -> false);
    }

    public static int cleanupOldEntries(long maxAgeMillis, BooleanSupplier shouldStop) {
        return cleanupOldEntriesInternal(maxAgeMillis, shouldStop).deleted;
    }

    private static CleanupResult cleanupOldEntriesInternal(long maxAgeMillis,
                                                          BooleanSupplier shouldStop) {
        if (maxAgeMillis <= 0) {
            return new CleanupResult(0, true);
        }

        BooleanSupplier stopCheck = shouldStop != null ? shouldStop : () -> false;
        List<byte[]> keysToDelete = new ArrayList<>();
        int[] skippedWithoutTimestamp = {0};
        long now = System.currentTimeMillis();

        boolean completed = DatabaseOperations.forEachEntryWhile((key, value) -> {
            if (stopCheck.getAsBoolean()) {
                return false;
            }

            try {
                if (isMetadataKey(key)) {
                    return true;
                }

                Long timestamp = extractStoredTimestamp(new String(value, StandardCharsets.UTF_8));
                if (timestamp == null || timestamp <= 0) {
                    skippedWithoutTimestamp[0]++;
                    return true;
                }

                if (now - timestamp > maxAgeMillis) {
                    keysToDelete.add(copyKey(key));
                }
            } catch (Exception e) {
                ModLogger.debug("Entree ignoree pendant cleanup: {}",
                        new String(key, StandardCharsets.UTF_8));
            }

            return !stopCheck.getAsBoolean();
        });

        int deleted = 0;
        if (completed) {
            deleted = deleteKeysInBatches(keysToDelete, stopCheck);
            completed = deleted == keysToDelete.size() && !stopCheck.getAsBoolean();
        }

        if (completed) {
            ModLogger.info("Nettoyage des traductions: {} entrees supprimees (plus vieilles que {} jours)",
                    deleted, (maxAgeMillis / DAY_MS));
        } else {
            ModLogger.info("Nettoyage des traductions interrompu: {} entrees supprimees",
                    deleted);
        }
        if (skippedWithoutTimestamp[0] > 0) {
            ModLogger.debug("Nettoyage des traductions: {} entrees sans timestamp conservees",
                    skippedWithoutTimestamp[0]);
        }
        return new CleanupResult(deleted, completed);
    }

    private static int deleteKeysInBatches(List<byte[]> keysToDelete,
                                           BooleanSupplier shouldStop) {
        if (keysToDelete == null || keysToDelete.isEmpty()) {
            return 0;
        }

        int deleted = 0;
        for (int start = 0; start < keysToDelete.size(); start += CLEANUP_DELETE_BATCH_SIZE) {
            if (shouldStop.getAsBoolean()) {
                break;
            }

            int end = Math.min(start + CLEANUP_DELETE_BATCH_SIZE, keysToDelete.size());
            try (WriteBatch batch = new WriteBatch()) {
                for (int i = start; i < end; i++) {
                    batch.delete(keysToDelete.get(i));
                }
                DatabaseOperations.writeBatch(batch);
                deleted += end - start;
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'application d'un batch cleanup RocksDB", e);
                break;
            }
        }

        return deleted;
    }

    private static byte[] copyKey(byte[] key) {
        return key == null ? new byte[0] : Arrays.copyOf(key, key.length);
    }

    private static long getLastCleanupTimestamp() {
        String storedValue = DatabaseOperations.get(LAST_CLEANUP_KEY);
        if (storedValue == null || storedValue.trim().isEmpty()) {
            return 0;
        }

        try {
            return Long.parseLong(storedValue.trim());
        } catch (NumberFormatException e) {
            ModLogger.debug("Timestamp de nettoyage cache invalide: {}", storedValue);
            return 0;
        }
    }

    private static void setLastCleanupTimestamp(long timestamp) {
        DatabaseOperations.put(LAST_CLEANUP_KEY, Long.toString(timestamp));
    }

    private static boolean isMetadataKey(byte[] key) {
        if (key == null) {
            return false;
        }
        return new String(key, StandardCharsets.UTF_8).startsWith(METADATA_PREFIX);
    }

    private static final class CleanupResult {
        private final int deleted;
        private final boolean completed;

        private CleanupResult(int deleted, boolean completed) {
            this.deleted = deleted;
            this.completed = completed;
        }
    }

    private static String encodeRawCacheValue(String translation) {
        String safeTranslation = translation != null ? translation : "";
        return GSON.toJson(new RawCachePayload(safeTranslation, System.currentTimeMillis()));
    }

    private static String decodeRawCacheValue(String storedValue) {
        RawCachePayload payload = parseRawCachePayload(storedValue);
        if (payload == null) {
            return storedValue;
        }
        return payload.translation != null ? payload.translation : "";
    }

    private static Long extractStoredTimestamp(String storedValue) {
        RawCachePayload rawPayload = parseRawCachePayload(storedValue);
        if (rawPayload != null) {
            return rawPayload.timestamp;
        }

        TranslationEntry entry = parseStoredTranslationEntry(storedValue);
        if (entry == null) {
            return null;
        }
        return entry.timestamp;
    }

    private static RawCachePayload parseRawCachePayload(String storedValue) {
        if (storedValue == null) {
            return null;
        }

        String trimmed = storedValue.trim();
        if (!trimmed.startsWith("{") || !trimmed.contains(RAW_CACHE_FORMAT)) {
            return null;
        }

        try {
            RawCachePayload payload = GSON.fromJson(trimmed, RawCachePayload.class);
            if (payload != null && RAW_CACHE_FORMAT.equals(payload.format)) {
                return payload;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static TranslationEntry parseStoredTranslationEntry(String storedValue) {
        if (storedValue == null) {
            return null;
        }

        String trimmed = storedValue.trim();
        if (!trimmed.startsWith("{") || trimmed.contains(RAW_CACHE_FORMAT)) {
            return null;
        }

        try {
            TranslationEntry entry = GSON.fromJson(trimmed, TranslationEntry.class);
            if (entry != null && entry.timestamp > 0) {
                return entry;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return DEFAULT_LANG;
        }
        return lang.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSourceLang(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return DEFAULT_SOURCE_LANG;
        }
        return lang.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeModel(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return DEFAULT_MODEL;
        }
        return modelId.trim().toLowerCase(Locale.ROOT);
    }

    private TranslationStorage() {
    }
}
