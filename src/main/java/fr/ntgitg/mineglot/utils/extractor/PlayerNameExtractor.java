package fr.ntgitg.mineglot.utils.extractor;

import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.detector.PlayerBotDetector;

import java.util.regex.Pattern;

public final class PlayerNameExtractor {

    private static final Pattern INVALID_CHARS = Pattern.compile("[^\\p{Alnum}_]");

    private PlayerNameExtractor() {
    }

    public static String extractBaseName(String formattedName) {
        if (formattedName == null || formattedName.isEmpty()) {
            return "";
        }

        String cleaned = cleanName(formattedName);
        if (!isValidPlayerName(cleaned)) {
            return "";
        }

        return cleaned;
    }

    public static String cleanName(String name) {
        if (name == null) {
            return "";
        }

        String result = name;
        result = ValidationService.MINECRAFT_COLOR_CODES.matcher(result).replaceAll("");
        result = INVALID_CHARS.matcher(result).replaceAll("");

        return result;
    }

    public static boolean isValidPlayerName(String name) {
        return ValidationService.isValidPlayerNameSimple(name);
    }

    public static boolean isBotName(String name) {
        return PlayerBotDetector.isBotName(name);
    }
}
