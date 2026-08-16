package fr.ntgitg.mineglot.core.translation.context;

import fr.ntgitg.mineglot.core.model.SupportedLanguage;

public final class TranslationContext {

    private final String sourceLanguageCode;
    private final SupportedLanguage detectedLanguage;
    private final String preparedText;
    private final String targetLanguageCode;

    public TranslationContext(String sourceLanguageCode, SupportedLanguage detectedLanguage,
                              String preparedText, String targetLanguageCode) {
        this.sourceLanguageCode = sourceLanguageCode;
        this.detectedLanguage = detectedLanguage;
        this.preparedText = preparedText;
        this.targetLanguageCode = targetLanguageCode;
    }

    public String getSourceLanguageCode() {
        return sourceLanguageCode;
    }

    public SupportedLanguage getDetectedLanguage() {
        return detectedLanguage;
    }

    public String getPreparedText() {
        return preparedText;
    }

    public String getEncodedText() {
        return preparedText;
    }

    public String getTargetLanguageCode() {
        return targetLanguageCode;
    }
}
