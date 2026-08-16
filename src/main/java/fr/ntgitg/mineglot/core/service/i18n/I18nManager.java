package fr.ntgitg.mineglot.core.service.i18n;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.help.HelpConfigLoader;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Properties;

public final class I18nManager {

    public static final String ENGLISH = "en";
    public static final String FRENCH = "fr";
    public static final String JAPANESE = "ja";

    private static final String LANG_PATH = "/assets/mineglot/lang/";
    private static final Map<String, String> LANGUAGE_FILES;

    static {
        Map<String, String> files = new HashMap<>();
        files.put(ENGLISH, "en_US.lang");
        files.put(FRENCH, "fr_FR.lang");
        files.put(JAPANESE, "ja_JP.lang");
        LANGUAGE_FILES = Collections.unmodifiableMap(files);
    }

    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, String> englishFallback = new HashMap<>();

    private volatile String currentLanguage = FRENCH;

    private I18nManager() {
    }

    private static I18nManager getInstance() {
        return SingletonManager.getInstance(I18nManager.class, I18nManager::new);
    }

    public static void initialize() {
        I18nManager manager = getInstance();
        manager.loadTranslationsInternal();
        manager.loadEnglishFallbackInternal();
        HelpConfigLoader.loadHelp(manager.currentLanguage);
        ModLogger.info("Interface du mod configuree en : {}", getLanguageDisplayName());
    }

    public static void syncWithConfig() {
        try {
            ConfigurationManager configManager = ConfigurationManager.getInstance();
            try {
                configManager.getConfig();
            } catch (IllegalStateException e) {
                ModLogger.warn("Configuration non disponible pour synchronisation I18nManager");
                return;
            }

            String targetLanguage = configManager.getUiLanguage();
            I18nManager manager = getInstance();
            if (!isLanguageSupported(targetLanguage)) {
                ModLogger.warn("Langue non supportee dans la configuration: {}", targetLanguage);
                return;
            }

            if (!manager.currentLanguage.equals(targetLanguage)) {
                ModLogger.info("Interface changee vers : {}",
                        getLanguageDisplayName(targetLanguage));
                manager.setLanguageInternal(targetLanguage);
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la synchronisation I18nManager", e);
        }
    }

    public static void setLanguage(String languageCode) {
        I18nManager manager = getInstance();
        if (!isLanguageSupported(languageCode)) {
            ModLogger.warn("Langue non supportee : {}", languageCode);
            return;
        }

        manager.setLanguageInternal(languageCode);
        ModLogger.info("Interface changee vers : {}", getLanguageDisplayName());
    }

    private synchronized void setLanguageInternal(String languageCode) {
        currentLanguage = languageCode;
        loadTranslationsInternal();
        HelpConfigLoader.loadHelp(currentLanguage);
    }

    public static boolean isLanguageSupported(String languageCode) {
        return LANGUAGE_FILES.containsKey(languageCode);
    }

    public static String getCurrentLanguage() {
        return getInstance().currentLanguage;
    }

    public static String getLanguageDisplayName() {
        return getLanguageDisplayName(getCurrentLanguage());
    }

    public static String getLanguageDisplayName(String languageCode) {
        if (ENGLISH.equals(languageCode)) {
            return "English";
        }
        if (FRENCH.equals(languageCode)) {
            return "Francais";
        }
        if (JAPANESE.equals(languageCode)) {
            return "Japanese";
        }
        return languageCode;
    }

    public static String[] getSupportedLanguages() {
        return LANGUAGE_FILES.keySet().toArray(new String[0]);
    }

    public static boolean isFrench() {
        return FRENCH.equals(getCurrentLanguage());
    }

    public static String getMessage(String key) {
        I18nManager manager = getInstance();
        manager.ensureTranslationsLoaded();

        String translation = manager.translations.get(key);
        if (translation != null) {
            return translation;
        }

        String englishTranslation = manager.englishFallback.get(key);
        if (englishTranslation != null) {
            ModLogger.warn("Cle de traduction non trouvee en {} : {} (fallback anglais)",
                    manager.currentLanguage, key);
            return englishTranslation;
        }

        ModLogger.warn("Cle de traduction non trouvee : {} (langue: {})", key,
                manager.currentLanguage);
        return key;
    }

    public static String getMessage(String key, Object... args) {
        String template = getMessage(key);
        if (args == null || args.length == 0) {
            return template;
        }

        try {
            return String.format(template, args);
        } catch (IllegalFormatException e) {
            ModLogger.warn("Erreur de format i18n pour la cle '{}': {}", key, e.getMessage());
            return template;
        }
    }

    private synchronized void ensureTranslationsLoaded() {
        if (translations.isEmpty()) {
            loadTranslationsInternal();
        }
        if (englishFallback.isEmpty()) {
            loadEnglishFallbackInternal();
        }
    }

    private synchronized void loadTranslationsInternal() {
        translations.clear();
        String langFile = LANGUAGE_FILES.get(currentLanguage);
        if (langFile == null) {
            ModLogger.error("Fichier de langue non configure pour : {}", currentLanguage);
            return;
        }

        try (InputStream is = I18nManager.class.getResourceAsStream(LANG_PATH + langFile)) {
            if (is == null) {
                ModLogger.error("Fichier de langue non trouve : {}", langFile);
                return;
            }

            Properties props = new Properties();
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                props.load(reader);
            }

            for (String key : props.stringPropertyNames()) {
                translations.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            ModLogger.error("Erreur lors du chargement de l'interface", e);
        }
    }

    private synchronized void loadEnglishFallbackInternal() {
        englishFallback.clear();

        try (InputStream is = I18nManager.class.getResourceAsStream(LANG_PATH + "en_US.lang")) {
            if (is == null) {
                ModLogger.warn("Fichier anglais non trouve pour le fallback");
                return;
            }

            Properties props = new Properties();
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                props.load(reader);
            }

            for (String key : props.stringPropertyNames()) {
                englishFallback.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            ModLogger.error("Erreur lors du chargement du fallback anglais", e);
        }
    }
}
