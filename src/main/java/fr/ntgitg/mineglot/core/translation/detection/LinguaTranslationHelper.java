package fr.ntgitg.mineglot.core.translation.detection;

import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.service.lingua.LinguaDetectorUtil;
import fr.ntgitg.mineglot.core.service.lingua.LinguaLanguageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.Optional;

public final class LinguaTranslationHelper {

    private LinguaTranslationHelper() {
    }

    public static String detectLanguageForTranslation(String text) {
        if (!LinguaLanguageService.getInstance().isLanguageDetectorReady()) {
            ModLogger.debug("Lingua pas encore pret pour traduction : '{}' -> 'auto'", text);
            return "auto";
        }

        Optional<String> detectedLang = LinguaDetectorUtil.detectIso(text);
        String sourceLang = detectedLang.orElse("auto");

        ModLogger.debug("Lingua detection pour traduction : '{}' -> '{}'", text, sourceLang);

        return sourceLang;
    }

    public static SupportedLanguage detectLanguageForTranslationDirect(String text) {
        if (!LinguaLanguageService.getInstance().isLanguageDetectorReady()) {
            ModLogger.debug("Lingua pas encore pret pour detection directe : '{}' -> auto", text);
            return null;
        }

        Optional<String> detectedLang = LinguaDetectorUtil.detectIso(text);

        if (detectedLang.isPresent()) {
            SupportedLanguage lang = SupportedLanguage.fromCode(detectedLang.get());
            if (lang != null) {
                ModLogger.debug("Lingua detection directe : '{}' -> '{}'", text, lang.getCode());
                return lang;
            }
        }

        ModLogger.debug("Lingua detection directe : '{}' -> non supporte", text);
        return null;
    }
}
