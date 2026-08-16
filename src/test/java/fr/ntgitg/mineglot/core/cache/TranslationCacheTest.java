package fr.ntgitg.mineglot.core.cache;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.storage.DatabaseOperations;
import fr.ntgitg.mineglot.core.storage.TranslationStorage;
import fr.ntgitg.mineglot.core.translation.TranslationService;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TranslationCacheTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void tearDown() {
        DatabaseOperations.cleanup();
        SingletonManager.removeInstance(DatabaseOperations.class);
        SingletonManager.removeInstance(TranslationCache.class);
        SingletonManager.removeInstance(TranslationService.class);
        ThreadManager.shutdown();
        SingletonManager.removeInstance(ThreadManager.class);
    }

    @Test
    public void unifiedLookupSuppressesImmediateRocksDbReadAfterRecentMiss() throws Exception {
        initTemporaryDatabase();
        TranslationCache cache = freshCache();
        String text = "hello cache";
        String targetLang = "fr";

        TranslationCache.UnifiedLookup first = cache.lookupUnified(text, targetLang);

        assertNull(first.getValue());

        TranslationStorage.putRawCacheValue(text, targetLang, "bonjour");

        TranslationCache.UnifiedLookup second = cache.lookupUnified(text, targetLang);

        assertNull(second.getValue());
        assertEquals(first.getKey(), second.getKey());

        cache.cacheTranslationAsyncByKey(first.getKey(), "bonjour").get(3, TimeUnit.SECONDS);

        TranslationCache.UnifiedLookup third = cache.lookupUnified(text, targetLang);

        assertEquals("bonjour", third.getValue());
    }

    @Test
    public void contextualLookupUsesSameRecentMissProtectionAndMemoryWritePath()
            throws Exception {
        initTemporaryDatabase();
        TranslationCache cache = freshCache();
        String key = cache.getCacheKey("hello", "en", "fr", "gpt-4o-mini");

        assertNull(cache.getCachedTranslationByKey(key));

        TranslationStorage.putRawCacheValueByKey(key, "salut");

        assertNull(cache.getCachedTranslationByKey(key));

        cache.cacheTranslationAsyncByKey(key, "salut").get(3, TimeUnit.SECONDS);

        assertEquals("salut", cache.getCachedTranslationByKey(key));
    }

    @Test
    public void clearLastTranslationInvalidatesPendingAsyncCacheWrite() throws Exception {
        initTemporaryDatabase();
        TranslationCache cache = freshCache();
        String key = cache.getCacheKey("hello", "en", "fr", "gpt-4o-mini");
        CountDownLatch dbTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseDbTask = new CountDownLatch(1);

        CompletableFuture<Void> blocker = ThreadManager.runDbAsync(() -> {
            dbTaskStarted.countDown();
            try {
                assertTrue(releaseDbTask.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(dbTaskStarted.await(3, TimeUnit.SECONDS));

        CompletableFuture<Void> pendingWrite = cache.cacheTranslationAsyncByKey(key, "salut");
        cache.setLastCacheKey(key);
        cache.setLastTranslatedText("hello");
        cache.setLastTargetLanguage("fr");
        cache.setLastTranslationTimestamp(System.currentTimeMillis());

        assertTrue(cache.clearLastTranslation());

        releaseDbTask.countDown();
        blocker.get(3, TimeUnit.SECONDS);
        pendingWrite.get(3, TimeUnit.SECONDS);

        assertNull(TranslationStorage.getRawCacheValueByKey(key).getValue());
        assertNull(cache.getCachedTranslationByKey(key));
    }

    @Test
    public void cacheMetricsUseExplicitVisibleResults() throws Exception {
        initTemporaryDatabase();
        TranslationCache cache = freshCache();

        assertEquals(1.0D, cache.getHitRate(), 0.0D);

        cache.recordCacheMiss();
        cache.recordCacheHit();

        assertEquals(1L, cache.getHitCount());
        assertEquals(1L, cache.getMissCount());
        assertEquals(0.5D, cache.getHitRate(), 0.0D);
    }

    private TranslationCache freshCache() {
        SingletonManager.removeInstance(TranslationCache.class);
        return TranslationCache.getInstance();
    }

    private void initTemporaryDatabase() throws Exception {
        DatabaseOperations.cleanup();
        SingletonManager.removeInstance(DatabaseOperations.class);

        File dbDir = temporaryFolder.newFolder("rocksdb");
        DatabaseOperations.configureDbPath(dbDir.getAbsolutePath());
        DatabaseOperations.init();
        DatabaseOperations.clear();
    }
}
