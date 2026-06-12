package fr.ntgitg.mineglot.utils.detector;

import fr.ntgitg.mineglot.core.validation.ValidationService;

import java.util.regex.Pattern;

public final class PlayerBotDetector {
    private PlayerBotDetector() {
    }

    private static final Pattern UUID_32 = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern UUID_36 =
            Pattern.compile("^[0-9a-fA-F]{8}(:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$");
    private static final Pattern MINECRAFT_NAME = ValidationService.VALID_MINECRAFT_NAME;

    public static boolean isBotName(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }

        if (UUID_32.matcher(name).matches() || UUID_36.matcher(name).matches()) {
            return true;
        }

        if (!MINECRAFT_NAME.matcher(name).matches()) {
                return true;
        }
        return false;
    }
}
