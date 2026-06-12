package fr.ntgitg.mineglot.core.validation;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.utils.detector.PlayerBotDetector;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.regex.Pattern;

public final class ValidationService {

    public static final Pattern VALID_MINECRAFT_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    public static final Pattern MINECRAFT_COLOR_CODES =
            Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");

    public static final int MAX_TRANSLATION_TEXT_LENGTH = 500;
    public static final int MAX_CHAT_MESSAGE_LENGTH = 256;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final int MIN_PLAYER_NAME_LENGTH = 3;
    public static final int MIN_API_KEY_LENGTH = 10;
    public static final int MAX_SIGN_CONTENT_LENGTH = 200;
    public static final int MAX_DISPLAY_TEXT_LENGTH = 50;

    private ValidationService() {
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String errorKey;

        private ValidationResult(boolean valid, String errorMessage, String errorKey) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorKey = errorKey;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(String errorMessage, String errorKey) {
            return new ValidationResult(false, errorMessage, errorKey);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorKey() {
            return errorKey;
        }

        @Override
        public String toString() {
            return valid ? "ValidationResult[SUCCESS]" : "ValidationResult[ERROR: " + errorMessage + "]";
        }
    }

    public static ValidationResult validateTranslationText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.error("Le texte ne peut pas être vide", "translation.error.empty");
        }

        if (text.length() > MAX_TRANSLATION_TEXT_LENGTH) {
            return ValidationResult.error(
                    "Le texte est trop long (max " + MAX_TRANSLATION_TEXT_LENGTH + " caractères)",
                    "translation.error.too_long");
        }

        return ValidationResult.success();
    }

    public static boolean isValidTranslationTextSimple(String text) {
        return text != null && !text.trim().isEmpty() && text.length() <= MAX_TRANSLATION_TEXT_LENGTH;
    }

    public static ValidationResult validateConversationText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.error("Le message ne peut pas être vide", "chat.error.empty");
        }

        if (text.length() > MAX_CHAT_MESSAGE_LENGTH) {
            return ValidationResult.error(
                    "Le message est trop long (max " + MAX_CHAT_MESSAGE_LENGTH + " caractères)",
                    "chat.error.too_long");
        }

        if (text.length() < 3) {
            return ValidationResult.error("Le message est trop court pour une conversation",
                    "conversation.error.too_short");
        }

        if (text.length() > 200) {
            return ValidationResult.error("Le message est trop long pour une conversation",
                    "conversation.error.too_long");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateChatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return ValidationResult.error("Le message ne peut pas être vide", "chat.error.empty");
        }

        if (message.length() > MAX_CHAT_MESSAGE_LENGTH) {
            return ValidationResult.error(
                    "Le message est trop long (max " + MAX_CHAT_MESSAGE_LENGTH + " caractères)",
                    "chat.error.too_long");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validatePlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return ValidationResult.error("Le nom de joueur ne peut pas être vide", "player.error.empty");
        }

        String cleanName = cleanMinecraftFormatting(playerName);

        if (cleanName.length() < MIN_PLAYER_NAME_LENGTH
                || cleanName.length() > MAX_PLAYER_NAME_LENGTH) {
            return ValidationResult.error("Le nom de joueur doit contenir entre " + MIN_PLAYER_NAME_LENGTH
                    + " et " + MAX_PLAYER_NAME_LENGTH + " caractères", "player.error.invalid_length");
        }

        if (!VALID_MINECRAFT_NAME.matcher(cleanName).matches()) {
            return ValidationResult.error("Le nom de joueur contient des caractères non autorisés",
                    "player.error.invalid_chars");
        }

        if (isLikelyBot(cleanName)) {
            return ValidationResult.error("Le nom semble appartenir à un bot",
                    "player.error.bot_detected");
        }

        return ValidationResult.success();
    }

    public static String cleanMinecraftFormatting(String text) {
        if (text == null)
            return "";
        return MINECRAFT_COLOR_CODES.matcher(text).replaceAll("");
    }

    public static boolean isLikelyBot(String playerName) {
        return PlayerBotDetector.isBotName(playerName);
    }

    private static volatile ValidationResult cachedConfigValidation;
    private static volatile long cachedConfigGeneration = -1L;

    public static ValidationResult validateTranslationConfiguration() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        long generation = configManager.getConfigGeneration();

        // La config ne change qu'au chargement / via les setters (qui incrementent
        // la generation). Tant qu'elle est stable, on reutilise le resultat valide
        // au lieu de tout revalider a chaque traduction.
        if (cachedConfigGeneration == generation) {
            ValidationResult cached = cachedConfigValidation;
            if (cached != null) {
                return cached;
            }
        }

        ValidationResult result = computeTranslationConfiguration(configManager);
        cachedConfigValidation = result;
        cachedConfigGeneration = generation;
        return result;
    }

    private static ValidationResult computeTranslationConfiguration(
            ConfigurationManager configManager) {
        try {
            String defaultLang = configManager.getDefaultLanguage();
            if (SupportedLanguage.fromCode(defaultLang) == null) {
                return ValidationResult.error("Langue par défaut non configurée ou invalide",
                        "config.error.no_default_lang");
            }

            String currentEngine = configManager.getCurrentEngine();
            String apiKey = configManager.getApiKey(currentEngine);

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ValidationResult.error("Aucune clé API configurée pour " + currentEngine,
                        "config.error.no_api_key");
            }

            if (apiKey.length() < MIN_API_KEY_LENGTH) {
                return ValidationResult.error("Clé API trop courte pour " + currentEngine,
                        "config.error.invalid_api_key");
            }

            return ValidationResult.success();

        } catch (Exception e) {
            ModLogger.error("Erreur lors de la validation de configuration", e);
            return ValidationResult.error("Erreur de configuration du mod", "config.error.general");
        }
    }

    public static String sanitizeUserMessage(String message) {
        if (message == null)
            return "";

        String sanitized = message;

        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 197) + "...";
        }

        return sanitized.trim();
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidPlayerNameSimple(String playerName) {
        return validatePlayerName(playerName).isValid();
    }

    public static boolean isValidApiKeySimple(String engine, String apiKey) {
        return fr.ntgitg.mineglot.core.model.base.ApiKeyValidationService.getInstance()
                .isValidApiKeySimple(engine, apiKey);
    }

    public static boolean hasMinimumConversationLength(String text) {
        return text != null && text.length() >= 3;
    }

    public static ValidationResult validateEngineName(String engineName) {
        if (engineName == null || engineName.trim().isEmpty()) {
            return ValidationResult.error("Le nom du moteur ne peut pas être vide", "engine.error.empty");
        }
        return ValidationResult.success();
    }

    public static boolean isValidEngineNameSimple(String engineName) {
        return validateEngineName(engineName).isValid();
    }

    public static ValidationResult validateDbPath(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return ValidationResult.error("Le chemin de base de données ne peut pas être vide",
                    "db.error.empty");
        }
        return ValidationResult.success();
    }

    public static boolean isValidDbPathSimple(String dbPath) {
        return validateDbPath(dbPath).isValid();
    }

    public static String truncateText(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    public static String truncateSignContent(String content) {
        return truncateText(content, MAX_SIGN_CONTENT_LENGTH);
    }

    public static String truncateDisplayText(String text) {
        return truncateText(text, MAX_DISPLAY_TEXT_LENGTH);
    }
}
