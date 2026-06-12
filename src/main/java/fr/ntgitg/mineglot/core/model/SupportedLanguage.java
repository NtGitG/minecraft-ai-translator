package fr.ntgitg.mineglot.core.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum SupportedLanguage {
    AUTO("auto", "Auto-detection", "XX", "Auto-detect"),

    ENGLISH("en", "English", "GB", "English"),
    FRENCH("fr", "Francais", "FR", "French"),
    GERMAN("de", "Deutsch", "DE", "German"),
    DUTCH("nl", "Nederlands", "NL", "Dutch"),
    CZECH("cs", "Cesky", "CZ", "Czech"),
    SPANISH("es", "Espanol", "ES", "Spanish"),
    ITALIAN("it", "Italiano", "IT", "Italian"),
    PORTUGUESE("pt", "Portugues", "PT", "Portuguese"),
    RUSSIAN("ru", "Russkiy", "RU", "Russian"),
    POLISH("pl", "Polski", "PL", "Polish"),
    TURKISH("tr", "Turkce", "TR", "Turkish"),
    ARABIC("ar", "Arabic", "SA", "Arabic"),
    HEBREW("he", "Ivrit", "IL", "Hebrew"),
    CHINESE("zh", "Chinese", "CN", "Chinese"),
    JAPANESE("ja", "Japanese", "JP", "Japanese"),
    KOREAN("ko", "Korean", "KR", "Korean"),
    HINDI("hi", "Hindi", "IN", "Hindi"),
    BENGALI("bn", "Bengali", "BD", "Bengali"),
    URDU("ur", "Urdu", "PK", "Urdu"),
    VIETNAMESE("vi", "Vietnamese", "VN", "Vietnamese"),
    INDONESIAN("id", "Indonesian", "ID", "Indonesian");

    private final String code;
    private final String displayName;
    private final String countryCode;
    private final String englishName;

    SupportedLanguage(String code, String displayName, String countryCode, String englishName) {
        this.code = code;
        this.displayName = displayName;
        this.countryCode = countryCode;
        this.englishName = englishName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getEnglishName() {
        return englishName;
    }

    private static final Map<String, SupportedLanguage> CODE_LOOKUP = new HashMap<>();

    static {
        for (SupportedLanguage lang : values()) {
            CODE_LOOKUP.put(lang.getCode().toLowerCase(Locale.ROOT), lang);
        }
    }

    public static SupportedLanguage fromCode(String code) {
        return code != null ? CODE_LOOKUP.get(code.toLowerCase(Locale.ROOT)) : null;
    }
}
