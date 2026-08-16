package fr.ntgitg.mineglot.core.translation;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;
import fr.ntgitg.mineglot.core.translation.context.TranslationContext;
import fr.ntgitg.mineglot.core.translation.context.TranslationContextResolver;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;


public final class TranslationOrchestrator {

    private final TranslationService translationService;
    private final ConfigurationManager configManager;

    private TranslationOrchestrator() {
        this.translationService = TranslationService.getInstance();
        this.configManager = ConfigurationManager.getInstance();
    }

    public static TranslationOrchestrator getInstance() {
        return SingletonManager.getInstance(TranslationOrchestrator.class,
                TranslationOrchestrator::new);
    }

    public static void translate(String sender, String text, boolean isTargetedPlayer,
                                 boolean isTranslationCommand) {
        getInstance().translateInternal(sender, text, isTargetedPlayer, isTranslationCommand, null,
                null);
    }

    public static void translate(String sender, String text, boolean isTargetedPlayer,
                                 boolean isTranslationCommand, SupportedLanguage detectedLang) {
        getInstance().translateInternal(sender, text, isTargetedPlayer, isTranslationCommand,
                detectedLang, null);
    }

    public static void translatePrivateMessage(String sender, String targetPlayer, String text) {
        getInstance().translateInternal(sender, text, false, false, null, targetPlayer);
    }

    public static CompletableFuture<Void> translateAsync(String sender, String text,
                                                         boolean isTargetedPlayer,
                                                         boolean isTranslationCommand) {
        return getInstance().translateAsyncInternal(sender, text, isTargetedPlayer,
                isTranslationCommand, null, null);
    }

    public static CompletableFuture<Void> translateAsync(String sender, String text,
                                                         boolean isTargetedPlayer,
                                                         boolean isTranslationCommand,
                                                         SupportedLanguage detectedLang) {
        return getInstance().translateAsyncInternal(sender, text, isTargetedPlayer,
                isTranslationCommand, detectedLang, null);
    }

    public static void translate(String text) {
        getInstance().translateInternal(null, text, false, false, null, null);
    }

    public static void translate(String text, boolean isTranslationCommand) {
        getInstance().translateInternal(null, text, true, isTranslationCommand, null, null);
    }

    private void translateInternal(String sender, String text, boolean isTargetedPlayer,
                                   boolean isTranslationCommand,
                                   SupportedLanguage detectedLang, String privateTarget) {
        if (text == null) {
            ModLogger.error("Tentative de traduction avec texte null");
            return;
        }
        translateAsyncInternal(sender, text, isTargetedPlayer, isTranslationCommand, detectedLang,
                privateTarget);
    }

    private CompletableFuture<Void> translateAsyncInternal(String sender, String text,
                                                           boolean isTargetedPlayer,
                                                           boolean isTranslationCommand,
                                                           SupportedLanguage detectedLang,
                                                           String privateTarget) {
        if (text == null) {
            ModLogger.error("Tentative de traduction async avec texte null");
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalArgumentException("text cannot be null"));
            return failedFuture;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        ModLogger.debug(
                "[ORCHESTRATOR] Parametres: sender='{}', text='{}', isTargetedPlayer={}, isTranslationCommand={}, detectedLang={}, privateTarget={}",
                sender, text, isTargetedPlayer, isTranslationCommand, detectedLang, privateTarget);

        if (!validateTranslationRequest(text)) {
            ModLogger.warn("Validation de la requete echouee");
            TranslationAsyncErrorHandler.failFuture(future,
                    new IllegalArgumentException("Validation failed"));
            return future;
        }

        boolean submitted = translationService.submitTranslation(() -> {
            try {
                // Fast-path: si la traduction est deja connue (texte + langue cible),
                // on l'affiche sans lancer la detection de langue (Lingua).
                if (detectedLang == null
                        && TranslationCacheApiFlow.tryUnifiedCacheHit(sender, text, isTargetedPlayer,
                                isTranslationCommand, privateTarget, configManager,
                                translationService, future)) {
                    return;
                }

                TranslationContext context = TranslationContextResolver.resolve(text, detectedLang,
                        isTargetedPlayer, configManager);

                ModLogger.debug("[ORCHESTRATOR] Contexte pret: source='{}', cible='{}', langue='{}'",
                        context.getSourceLanguageCode(), context.getTargetLanguageCode(),
                        context.getDetectedLanguage());

                ModLogger.debug("=== DEBUT TRADUCTION ===");
                ModLogger.debug("Texte: '{}' | Langue: '{}' | Expediteur: '{}'", text,
                        context.getSourceLanguageCode(), sender);

                processTranslationRequest(sender, text, context, isTargetedPlayer,
                        isTranslationCommand, privateTarget, future);
            } catch (Exception e) {
                ModLogger.error("Erreur dans processTranslationRequest", e);
                TranslationAsyncErrorHandler.handleManagedError(e, ErrorType.TRANSLATION);
                TranslationAsyncErrorHandler.failFuture(future, e);
            }
        });

        if (!submitted) {
            TranslationAsyncErrorHandler.failFuture(future,
                    new IllegalStateException("Translation service not operational"));
        }

        return future;
    }

    private void processTranslationRequest(String sender, String text,
                                           TranslationContext context,
                                           boolean isTargetedPlayer,
                                           boolean isTranslationCommand,
                                           String privateTarget,
                                           CompletableFuture<Void> future) {
        TranslationCacheApiFlow.execute(sender, text, context, isTargetedPlayer,
                isTranslationCommand, privateTarget, configManager, translationService, future);
    }

    private boolean validateTranslationRequest(String text) {
        ValidationService.ValidationResult textValidation =
                ValidationService.validateTranslationText(text);

        if (!textValidation.isValid()) {
            String errorKey = textValidation.getErrorKey() != null
                    ? textValidation.getErrorKey()
                    : "translation.error.general";
            ThreadSafeMessageService.sendError(errorKey);
            return false;
        }

        ValidationService.ValidationResult configValidation =
                ValidationService.validateTranslationConfiguration();

        if (!configValidation.isValid()) {
            String errorKey = configValidation.getErrorKey() != null
                    ? configValidation.getErrorKey()
                    : "config.error.general";
            ThreadSafeMessageService.sendError(errorKey);
            return false;
        }

        return true;
    }
}
