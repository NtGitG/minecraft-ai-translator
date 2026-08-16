package fr.ntgitg.mineglot.core.translation;

import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

public final class TranslationAsyncErrorHandler {

    private TranslationAsyncErrorHandler() {
    }

    public static void handleManagedError(Throwable error, ErrorType errorType) {
        boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
            try {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft == null || minecraft.thePlayer == null) {
                    ModLogger.warn("Gestion d'erreur ignoree: joueur Minecraft indisponible");
                    return;
                }
                ErrorManager.handleError(asException(error), errorType, minecraft.thePlayer);
            } catch (Exception handlerError) {
                ModLogger.error("Erreur lors de la gestion d'erreur sur le thread principal",
                        handlerError);
            }
        });
        if (!scheduled) {
            ModLogger.warn("Gestion d'erreur ignoree: thread principal Minecraft indisponible");
        }
    }

    public static void failAndReset(TranslationService translationService,
                                    CompletableFuture<Void> future,
                                    Throwable error,
                                    ErrorType errorType) {
        handleManagedError(error, errorType);
        markFailure(translationService);
        failFuture(future, error);
    }

    public static void failWithMessage(TranslationService translationService,
                                       CompletableFuture<Void> future,
                                       String messageKey,
                                       Throwable error) {
        ThreadSafeMessageService.sendError(messageKey);
        markFailure(translationService);
        failFuture(future, error);
    }

    public static void failFuture(CompletableFuture<Void> future, Throwable error) {
        if (future != null && !future.isDone()) {
            future.completeExceptionally(error != null ? error
                    : new RuntimeException("Translation failed"));
        }
    }

    private static void markFailure(TranslationService translationService) {
        if (translationService != null) {
            translationService.setTranslationInProgress(false);
        }
    }

    private static Exception asException(Throwable error) {
        if (error instanceof Exception) {
            return (Exception) error;
        }
        String message = error != null ? error.getMessage() : "Unknown translation error";
        return new Exception(message, error);
    }
}
