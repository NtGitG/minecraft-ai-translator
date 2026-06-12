package fr.ntgitg.mineglot.utils.clean;

import fr.ntgitg.mineglot.core.validation.ValidationService;

public final class MessageCleaner {

    private MessageCleaner() {
    }

    public static String clean(String message) {
        if (!ValidationService.isNotEmpty(message)) {
            return "";
        }

        return ValidationService.MINECRAFT_COLOR_CODES.matcher(message).replaceAll("");
    }
}
