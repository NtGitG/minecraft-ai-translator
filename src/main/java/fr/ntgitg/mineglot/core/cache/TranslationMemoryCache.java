package fr.ntgitg.mineglot.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

final class TranslationMemoryCache {

    private static final int CACHE_EXPIRE_MINUTES = 30;

    private final Cache<String, String> translationCache;
    private final AtomicLong currentWeightBytes = new AtomicLong(0);

    TranslationMemoryCache(long maxBytes) {
        translationCache = CacheBuilder.newBuilder()
                .maximumWeight(maxBytes)
                .weigher((String k, String v) -> (int) weightFor(k, v))
                .expireAfterAccess(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .removalListener(this::handleTranslationRemoval)
                .concurrencyLevel(4)
                .build();

    }

    String getTranslation(String key) {
        return translationCache.getIfPresent(key);
    }

    void putTranslation(String key, String translation) {
        currentWeightBytes.addAndGet(weightFor(key, translation));
        translationCache.put(key, translation);
    }

    void removeTranslation(String key) {
        translationCache.invalidate(key);
    }

    void clearAll() {
        translationCache.invalidateAll();
        currentWeightBytes.set(0);
    }

    long getCacheSize() {
        return translationCache.size();
    }

    long getMemoryBytes() {
        return currentWeightBytes.get();
    }

    private void handleTranslationRemoval(RemovalNotification<String, String> notification) {
        String key = notification.getKey();
        String value = notification.getValue();
        if (key == null || value == null) {
            return;
        }

        long removedWeight = weightFor(key, value);
        currentWeightBytes.updateAndGet(current -> Math.max(0, current - removedWeight));
    }

    private static long weightFor(String key, String value) {
        return key.getBytes(StandardCharsets.UTF_8).length
                + value.getBytes(StandardCharsets.UTF_8).length;
    }
}
