package fr.ntgitg.mineglot.ui.gui.utils.title;

import fr.ntgitg.mineglot.core.service.i18n.I18nManager;

public final class TitleManager {
    private static final String DEFAULT_TITLE_COLOR = "\u00A7l\u00A7f";

    private TitleManager() {
    }

    private static String formatTitle(String messageKey) {
        return DEFAULT_TITLE_COLOR + I18nManager.getMessage(messageKey);
    }

    private static String formatTitle(String messageKey, String color) {
        return color + I18nManager.getMessage(messageKey);
    }

    public static String getApiKeyTitle(String engine) {
        return formatTitle("api_key.title") + " (" + engine + ")";
    }

    public static String getAuthorTitle() {
        return formatTitle("author.title");
    }

    public static String getCacheTitle() {
        return formatTitle("cache.title");
    }

    public static String getConfigTitle() {
        return formatTitle("gui.title");
    }

    public static String getCreditsTitle() {
        return formatTitle("credits.title");
    }

    public static String getLanguageTitle() {
        return formatTitle("language.title");
    }

    public static String getDefaultLanguageTitle() {
        return formatTitle("language.default");
    }

    public static String getMainTitle() {
        return formatTitle("menu.title");
    }

    public static String getHelpTitle() {
        return formatTitle("help.title");
    }

    public static String getTargetTitle() {
        return formatTitle("target.title");
    }

    public static String getModelTitle() {
        return formatTitle("model.title");
    }

    public static String getCustomTitle(String messageKey, String color) {
        return formatTitle(messageKey, color);
    }
}
