package fr.ntgitg.mineglot.core.cache;

final class TranslationCacheState {

    private static final long RECENT_TRANSLATION_WINDOW_MS = 5L * 60L * 1000L;

    private volatile String lastTranslatedText;
    private volatile String lastTargetLanguage;
    private volatile String lastCacheKey;
    private volatile long lastTranslationTimestamp;
    private volatile boolean trsClearCommandUsed;

    boolean hasRecentTranslation() {
        if (lastTranslatedText == null || lastTargetLanguage == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastTranslationTimestamp) < RECENT_TRANSLATION_WINDOW_MS;
    }

    boolean isTrsClearCommandUsed() {
        return trsClearCommandUsed;
    }

    void setTrsClearCommandUsed(boolean used) {
        trsClearCommandUsed = used;
    }

    void setLastTranslatedText(String text) {
        lastTranslatedText = text;
    }

    void setLastTargetLanguage(String targetLanguage) {
        lastTargetLanguage = targetLanguage;
    }

    void setLastCacheKey(String cacheKey) {
        lastCacheKey = cacheKey;
    }

    void setLastTranslationTimestamp(long timestamp) {
        lastTranslationTimestamp = timestamp;
    }

    long getLastTranslationTimestamp() {
        return lastTranslationTimestamp;
    }

    String getLastTranslatedText() {
        return lastTranslatedText;
    }

    String getLastTargetLanguage() {
        return lastTargetLanguage;
    }

    String getLastCacheKey() {
        return lastCacheKey;
    }

    void resetTrsClearState() {
        lastTranslatedText = null;
        lastTargetLanguage = null;
        lastCacheKey = null;
        lastTranslationTimestamp = 0;
        trsClearCommandUsed = false;
    }
}
