package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ApiKeyValidationService {
    private final Map<String, ApiKeyValidator> validators = new HashMap<>();

    private static final int MIN_API_KEY_LENGTH = 10;

    private ApiKeyValidationService() {
        try {
            validators.put("openai", NetworkApiKeyTester.createOpenAI());
            validators.put("claude", NetworkApiKeyTester.createClaude());
        } catch (Exception e) {
            ModLogger.error("Erreur dans ApiKeyValidationService constructeur", e);
            ModLogger.error("Type d'erreur : {} - Message : {}", e.getClass().getSimpleName(),
                    e.getMessage());
            throw new RuntimeException("Impossible d'initialiser ApiKeyValidationService", e);
        }
    }

    public static ApiKeyValidationService getInstance() {
        try {
            ApiKeyValidationService instance =
                    SingletonManager.getInstance(ApiKeyValidationService.class, ApiKeyValidationService::new);
            return instance;
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'initialisation du service de validation des clés API", e);
            ModLogger.error("Type d'erreur : {} - Message : {}", e.getClass().getSimpleName(),
                    e.getMessage());
            throw new RuntimeException("Impossible d'initialiser le service de validation des clés API",
                    e);
        }
    }

    public void validateApiKey(String engine, String key) {
        if (key == null || key.isEmpty()) {
            return; // Les clés vides sont autorisées
        }

        ApiKeyValidator validator = getValidatorForEngine(engine);
        if (validator == null) {
            ModLogger.warn("Aucun validateur trouvé pour le moteur: " + engine);
            return;
        }

        try {
            validator.validateKey(key);
        } catch (IllegalArgumentException e) {
            ModLogger.error("Cle API invalide - Moteur: {}, Longueur: {}, Erreur: {}", engine,
                    key != null ? key.length() : 0, e.getMessage(), e);
            throw new IllegalArgumentException("Cle API invalide pour " + engine + ": " + e.getMessage(),
                    e);
        }
    }

    public ValidationService.ValidationResult validateApiKeyWithResult(String engine, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ValidationService.ValidationResult.error("La clé API ne peut pas être vide",
                    "api.error.empty");
        }

        if (apiKey.length() < MIN_API_KEY_LENGTH) {
            return ValidationService.ValidationResult.error("La clé API est trop courte",
                    "api.error.too_short");
        }

        ApiKeyValidator validator = getValidatorForEngine(engine);
        if (validator == null) {
            return ValidationService.ValidationResult.error("Moteur d'IA non supporté: " + engine,
                    "api.error.unsupported_engine");
        }

        try {
            validator.validateKey(apiKey);
        } catch (IllegalArgumentException e) {
            return ValidationService.ValidationResult.error("Format de clé API invalide pour " + engine,
                    "api.error.invalid_format");
        }

        return ValidationService.ValidationResult.success();
    }

    public boolean isValidApiKeyFormatSimple(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.length() > MIN_API_KEY_LENGTH
                && apiKey.startsWith("sk-");
    }

    public boolean isValidApiKeySimple(String engine, String apiKey) {
        return validateApiKeyWithResult(engine, apiKey).isValid();
    }

    public void testApiKey(String engine, String key) throws IOException {
        if (key == null || key.isEmpty()) {
            return;
        }

        ApiKeyValidator validator = getValidatorForEngine(engine);
        if (validator == null) {
            ModLogger.warn("Aucun validateur trouvé pour le moteur: " + engine);
            return;
        }

        try {
            validator.testApiKey(key);
        } catch (IOException e) {
            ModLogger.error("Test cle API echoue - Moteur: {}, Validator: {}, Erreur: {}", engine,
                    validator.getClass().getSimpleName(), e.getMessage(), e);
            throw new IOException("Test de la cle API " + engine + " echoue: " + e.getMessage(), e);
        }
    }

    private ApiKeyValidator getValidatorForEngine(String engine) {
        if (engine == null) {
            return null;
        }
        return validators.get(engine.toLowerCase(java.util.Locale.ROOT));
    }
}
