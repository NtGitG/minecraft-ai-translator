package fr.ntgitg.mineglot.utils.text;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String toLowerCase(String text) {
        if (text == null) {
            return null;
        }
        return text.toLowerCase(Locale.ROOT);
    }

    public static String toLowerCaseAndTrim(String text) {
        if (text == null) {
            return null;
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean equalsIgnoreCase(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return text1 == text2;
        }
        return text1.toLowerCase(Locale.ROOT).equals(text2.toLowerCase(Locale.ROOT));
    }

    public static boolean containsIgnoreCase(String text, String searchText) {
        if (text == null || searchText == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT));
    }
}
