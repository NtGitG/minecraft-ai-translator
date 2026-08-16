package fr.ntgitg.mineglot.core.apikey;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.base.ApiKeyValidationService;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;

import java.io.IOException;
import java.util.function.Consumer;

public class ApiKeyCoordinator {

    private static final ConfigurationManager configManager = ConfigurationManager.getInstance();
    private static final ApiKeyValidationService validationService =
            ApiKeyValidationService.getInstance();

    public static void validateAndSaveApiKey(String engine, String apiKey, Runnable onSuccess,
                                             Consumer<String> onError) {
        try {
            ValidationService.ValidationResult validation =
                    validationService.validateApiKeyWithResult(engine, apiKey);
            if (!validation.isValid()) {
                String errorKey =
                        validation.getErrorKey() != null ? validation.getErrorKey() : "api_key.save_error";
                ThreadSafeMessageService.sendError(errorKey);
                if (onError != null) {
                    onError.accept(errorKey);
                }
                return;
            }
            configManager.setApiKey(engine, apiKey);
            ThreadSafeMessageService.sendSuccess("api_key.save_success");
            if (onSuccess != null)
                onSuccess.run();
        } catch (IllegalArgumentException e) {
            ModLogger.warn("Validation cle API echouee - Moteur: {}, Erreur: {}", engine, e.getMessage());
            ThreadSafeMessageService.sendError("api_key.save_error");
            if (onError != null)
                onError.accept("api_key.save_error");
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la sauvegarde de la cle API", e);
            ThreadSafeMessageService.sendError("api_key.save_error");
            if (onError != null)
                onError.accept("api_key.save_error");
        }
    }

    public static void testApiKeyAsync(String engine, String apiKey, Runnable onSuccess,
                                       Consumer<String> onError) {
        ThreadManager.runAsync(() -> {
            try {
                ValidationService.ValidationResult validation =
                        validationService.validateApiKeyWithResult(engine, apiKey);
                if (!validation.isValid()) {
                    String errorKey =
                            validation.getErrorKey() != null ? validation.getErrorKey() : "api_key.test_error";
                    ThreadSafeMessageService.sendError(errorKey);
                    if (onError != null) {
                        onError.accept(errorKey);
                    }
                    return;
                }
                validationService.testApiKey(engine, apiKey);
                ThreadSafeMessageService.sendSuccess("api_key.test_success");
                if (onSuccess != null)
                    onSuccess.run();
            } catch (IOException e) {
                ModLogger.warn("Test cle API echoue - Moteur: {}, Erreur: {}", engine, e.getMessage());
                ThreadSafeMessageService.sendError("api_key.test_error");
                if (onError != null)
                    onError.accept("api_key.test_error");
            } catch (Exception e) {
                ModLogger.error("Erreur lors du test de la cle API", e);
                ThreadSafeMessageService.sendError("api_key.test_error");
                if (onError != null)
                    onError.accept(e.getMessage());
            }
        });
    }

    public static void clearApiKey(String engine, Runnable onSuccess, Consumer<String> onError) {
        try {
            configManager.setApiKey(engine, "");
            ThreadSafeMessageService.sendInfo("api_key.cleared");
            if (onSuccess != null)
                onSuccess.run();
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'effacement de la cle API", e);
            ThreadSafeMessageService.sendError("api_key.clear_error");
            if (onError != null)
                onError.accept("api_key.clear_error");
        }
    }

    private ApiKeyCoordinator() {
    }
}
