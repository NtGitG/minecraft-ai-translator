package fr.ntgitg.mineglot.utils.text;

import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.utils.clean.MessageCleaner;
import fr.ntgitg.mineglot.utils.encoder.LanguageEncoder;

public final class TranslationTextPreparer {

    private TranslationTextPreparer() {
    }

    public static String prepare(String text, SupportedLanguage sourceLanguage) {
        String cleanText = MessageCleaner.clean(text);
        if (cleanText.isEmpty() || sourceLanguage == null) {
            return cleanText;
        }

        return LanguageEncoder.encode(cleanText, sourceLanguage);
    }

    public static String prepare(String text, String sourceLanguageCode) {
        SupportedLanguage sourceLanguage = SupportedLanguage.fromCode(sourceLanguageCode);
        return prepare(text, sourceLanguage);
    }
}
