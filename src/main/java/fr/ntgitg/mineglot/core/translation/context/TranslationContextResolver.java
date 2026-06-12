package fr.ntgitg.mineglot.core.translation.context;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.translation.detection.LinguaTranslationHelper;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import fr.ntgitg.mineglot.utils.text.TranslationTextPreparer;

public final class TranslationContextResolver {

    private TranslationContextResolver() {
    }

    public static TranslationContext resolve(String text, SupportedLanguage preDetectedLanguage,
                                             boolean targetedPlayer,
                                             ConfigurationManager configurationManager) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if (configurationManager == null) {
            throw new IllegalArgumentException("configurationManager cannot be null");
        }

        SupportedLanguage detectedLanguage = resolveDetectedLanguage(text, preDetectedLanguage);
        String sourceLanguageCode = detectedLanguage != null ? detectedLanguage.getCode() : "auto";
        String preparedText = resolvePreparedText(text, detectedLanguage);
        String targetLanguageCode = resolveTargetLanguageCode(targetedPlayer, configurationManager);

        return new TranslationContext(sourceLanguageCode, detectedLanguage, preparedText,
                targetLanguageCode);
    }

    private static SupportedLanguage resolveDetectedLanguage(String text,
                                                             SupportedLanguage preDetectedLanguage) {
        if (preDetectedLanguage != null) {
            ModLogger.debug("[ORCHESTRATOR] Utilisation de la langue predetectee: '{}'",
                    preDetectedLanguage);
            return preDetectedLanguage;
        }

        SupportedLanguage detected = LinguaTranslationHelper.detectLanguageForTranslationDirect(text);
        ModLogger.debug("[ORCHESTRATOR] Langue detectee automatiquement: '{}'", detected);
        return detected;
    }

    private static String resolvePreparedText(String text, SupportedLanguage detectedLanguage) {
        if (detectedLanguage == null) {
            ModLogger.debug("Pas d'encodage RTL - langue source 'auto', API gere la detection");
            return TranslationTextPreparer.prepare(text, (SupportedLanguage) null);
        }

        String preparedText = TranslationTextPreparer.prepare(text, detectedLanguage);
        if (preparedText == null || preparedText.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Encoding failed for language %s", detectedLanguage.getCode()));
        }

        ModLogger.debug("Texte encode avec langue detectee: {}", detectedLanguage.getCode());
        return preparedText;
    }

    public static String resolveTargetLanguageCode(boolean targetedPlayer,
                                                    ConfigurationManager configurationManager) {
        String configuredTargetLanguage = configurationManager.getTargetLanguage();
        if (SupportedLanguage.fromCode(configuredTargetLanguage) == null) {
            throw new IllegalStateException("Target language not configured");
        }

        String finalTargetLanguage = targetedPlayer
                ? configurationManager.getDefaultLanguage()
                : configuredTargetLanguage;

        if (SupportedLanguage.fromCode(finalTargetLanguage) == null) {
            throw new IllegalStateException("Final target language not configured");
        }

        return finalTargetLanguage;
    }
}
