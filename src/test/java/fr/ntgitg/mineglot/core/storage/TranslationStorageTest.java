package fr.ntgitg.mineglot.core.storage;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TranslationStorageTest {

    private static final long DAY_MS = 86_400_000L;
    private static final String LAST_CLEANUP_KEY = "__mineglot_meta_last_cleanup_ms";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void tearDownDatabase() {
        DatabaseOperations.cleanup();
        SingletonManager.removeInstance(DatabaseOperations.class);
    }

    @Test
    public void contextualCacheKeyIsStableForSameContext() {
        String first = TranslationStorage.generateContextualCacheKey("GG", "en", "fr",
                "gpt-4o-mini");
        String second = TranslationStorage.generateContextualCacheKey("GG", "en", "fr",
                "gpt-4o-mini");

        assertEquals(first, second);
    }

    @Test
    public void contextualCacheKeyChangesWhenSourceLanguageChanges() {
        String englishSource = TranslationStorage.generateContextualCacheKey("chat", "en", "fr",
                "gpt-4o-mini");
        String frenchSource = TranslationStorage.generateContextualCacheKey("chat", "fr", "fr",
                "gpt-4o-mini");

        assertNotEquals(englishSource, frenchSource);
    }

    @Test
    public void contextualCacheKeyChangesWhenTargetLanguageChanges() {
        String frenchTarget = TranslationStorage.generateContextualCacheKey("hello", "en", "fr",
                "gpt-4o-mini");
        String spanishTarget = TranslationStorage.generateContextualCacheKey("hello", "en", "es",
                "gpt-4o-mini");

        assertNotEquals(frenchTarget, spanishTarget);
    }

    @Test
    public void contextualCacheKeyChangesWhenModelChanges() {
        String openAi = TranslationStorage.generateContextualCacheKey("hello", "en", "fr",
                "gpt-4o-mini");
        String claude = TranslationStorage.generateContextualCacheKey("hello", "en", "fr",
                "claude-3-sonnet-20240229");

        assertNotEquals(openAi, claude);
    }

    @Test
    public void contextualCacheKeyNormalizesTargetLanguageAndModelCase() {
        String first = TranslationStorage.generateContextualCacheKey("hello", "EN", " FR ",
                "GPT-4O-MINI");
        String second = TranslationStorage.generateContextualCacheKey("hello", "en", "fr",
                "gpt-4o-mini");

        assertEquals(first, second);
        assertTrue(first.endsWith("_fr"));
    }

    @Test
    public void contextualCacheKeyDoesNotReuseLegacyKey() {
        String legacy = TranslationStorage.generateUnifiedCacheKey("hello", "fr");
        String contextual = TranslationStorage.generateContextualCacheKey("hello", "en", "fr",
                "gpt-4o-mini");

        assertNotEquals(legacy, contextual);
    }

    @Test
    public void rawCacheValueIsStoredWithTimestampPayloadAndDecoded() throws Exception {
        initTemporaryDatabase();
        String key = TranslationStorage.generateUnifiedCacheKey("hello", "fr");

        TranslationStorage.putRawCacheValueByKey(key, "bonjour");

        TranslationStorage.RawCacheResult result = TranslationStorage.getRawCacheValueByKey(key);
        String storedValue = DatabaseOperations.get(key);

        assertEquals("bonjour", result.getValue());
        assertFalse(result.isLegacy());
        assertNotNull(storedValue);
        assertTrue(storedValue.contains("mineglot_raw_v1"));
        assertTrue(storedValue.contains("\"timestamp\""));
    }

    @Test
    public void legacyRawCacheValueIsReturnedAndFlaggedForMigration() throws Exception {
        initTemporaryDatabase();
        String legacyKey = TranslationStorage.generateCacheKey("Hello", "FR");
        DatabaseOperations.put(legacyKey, "salut");

        TranslationStorage.RawCacheResult result = TranslationStorage.getRawCacheValue("Hello",
                "fr");

        assertEquals("salut", result.getValue());
        assertTrue(result.isLegacy());
    }

    @Test
    public void cleanupOldEntriesDeletesExpiredRawAndStructuredEntriesOnly() throws Exception {
        initTemporaryDatabase();
        long now = System.currentTimeMillis();
        DatabaseOperations.put("old-raw", rawPayload("old", now - (2 * DAY_MS)));
        DatabaseOperations.put("fresh-raw", rawPayload("fresh", now));
        DatabaseOperations.put("old-entry", structuredEntryPayload(now - (2 * DAY_MS)));
        DatabaseOperations.put("__mineglot_meta_custom", "keep");
        DatabaseOperations.put("without-timestamp", "{\"translation\":\"keep\"}");

        int deleted = TranslationStorage.cleanupOldEntries(DAY_MS);

        assertEquals(2, deleted);
        assertNull(DatabaseOperations.get("old-raw"));
        assertNull(DatabaseOperations.get("old-entry"));
        assertNotNull(DatabaseOperations.get("fresh-raw"));
        assertNotNull(DatabaseOperations.get("__mineglot_meta_custom"));
        assertNotNull(DatabaseOperations.get("without-timestamp"));
    }

    @Test
    public void cleanupOldEntriesIfDueStoresTimestampAndSkipsRecentRuns() throws Exception {
        initTemporaryDatabase();
        long oldTimestamp = System.currentTimeMillis() - (2 * DAY_MS);
        DatabaseOperations.put("old-first", rawPayload("old-first", oldTimestamp));

        boolean firstRunCompleted = TranslationStorage.cleanupOldEntriesIfDue(DAY_MS, DAY_MS);

        assertTrue(firstRunCompleted);
        assertNull(DatabaseOperations.get("old-first"));
        assertNotNull(DatabaseOperations.get(LAST_CLEANUP_KEY));

        DatabaseOperations.put("old-second", rawPayload("old-second", oldTimestamp));

        boolean secondRunCompleted = TranslationStorage.cleanupOldEntriesIfDue(DAY_MS, DAY_MS);

        assertFalse(secondRunCompleted);
        assertNotNull(DatabaseOperations.get("old-second"));
    }

    @Test
    public void cleanupOldEntriesIfDueDoesNotStoreTimestampWhenCancelled() throws Exception {
        initTemporaryDatabase();
        long oldTimestamp = System.currentTimeMillis() - (2 * DAY_MS);
        DatabaseOperations.put("old-cancelled", rawPayload("old-cancelled", oldTimestamp));

        boolean completed = TranslationStorage.cleanupOldEntriesIfDue(DAY_MS, 0L, () -> true);

        assertFalse(completed);
        assertNotNull(DatabaseOperations.get("old-cancelled"));
        assertNull(DatabaseOperations.get(LAST_CLEANUP_KEY));
    }

    @Test
    public void cleanupOldEntriesDeletesMoreThanOneBatch() throws Exception {
        initTemporaryDatabase();
        long oldTimestamp = System.currentTimeMillis() - (2 * DAY_MS);
        for (int i = 0; i < 505; i++) {
            DatabaseOperations.put("old-batch-" + i, rawPayload("old-" + i, oldTimestamp));
        }

        int deleted = TranslationStorage.cleanupOldEntries(DAY_MS);

        assertEquals(505, deleted);
        assertNull(DatabaseOperations.get("old-batch-0"));
        assertNull(DatabaseOperations.get("old-batch-500"));
        assertNull(DatabaseOperations.get("old-batch-504"));
    }

    private void initTemporaryDatabase() throws Exception {
        DatabaseOperations.cleanup();
        SingletonManager.removeInstance(DatabaseOperations.class);

        File dbDir = temporaryFolder.newFolder("rocksdb");
        DatabaseOperations.configureDbPath(dbDir.getAbsolutePath());
        DatabaseOperations.init();
        DatabaseOperations.clear();
    }

    private static String rawPayload(String translation, long timestamp) {
        return "{\"format\":\"mineglot_raw_v1\",\"translation\":\"" + translation
                + "\",\"timestamp\":" + timestamp + "}";
    }

    private static String structuredEntryPayload(long timestamp) {
        return "{\"originalText\":\"old\",\"correctedText\":\"old\",\"translation\":\"vieux\","
                + "\"targetLang\":\"fr\",\"timestamp\":" + timestamp + "}";
    }
}
