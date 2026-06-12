package fr.ntgitg.mineglot.core.translation;

import fr.ntgitg.mineglot.core.cache.TranslationCache;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.base.AbstractAIEngine;
import fr.ntgitg.mineglot.core.model.base.RetryPolicy;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;
import fr.ntgitg.mineglot.core.translation.context.TranslationContext;
import fr.ntgitg.mineglot.core.translation.context.TranslationContextResolver;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class TranslationCacheApiFlow {

    private TranslationCacheApiFlow() {
    }

    /**
     * Tentative de cache "rapide" basee uniquement sur (texte + langue cible),
     * effectuee AVANT toute detection de langue (Lingua).
     *
     * <p>Si une traduction est deja connue, on l'affiche immediatement sans payer
     * la detection de langue. En cas d'absence ou de probleme, on renvoie false
     * pour laisser le flux complet (detection + cache contextuel + API) s'executer.</p>
     *
     * @return true si un hit a ete traite (rien d'autre a faire), false sinon.
     */
    public static boolean tryUnifiedCacheHit(String sender, String text, boolean isTargetedPlayer,
                                             boolean isTranslationCommand,
                                             String privateTarget,
                                             ConfigurationManager configurationManager,
                                             TranslationService translationService,
                                             CompletableFuture<Void> future) {
        try {
            String targetLangCode = TranslationContextResolver.resolveTargetLanguageCode(
                    isTargetedPlayer, configurationManager);

            TranslationCache cache = TranslationCache.getInstance();
            TranslationCache.UnifiedLookup lookup = cache.lookupUnified(text, targetLangCode);
            String cachedResult = lookup.getValue();
            if (cachedResult == null) {
                return false;
            }

            cache.recordCacheHit();
            ModLogger.debug("[FAST-CACHE] Hit sans detection de langue pour: '{}'", text);

            String cacheKey = lookup.getKey();
            translationService.setTranslationInProgress(true);
            updateLastTranslationState(cache, text, targetLangCode, cacheKey,
                    canUseTrsClear(isTargetedPlayer));

            boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
                try {
                    if (!TranslationRenderer.renderTranslation(sender, text, cachedResult,
                            isTargetedPlayer, isTranslationCommand, privateTarget)) {
                        failRender(translationService, future);
                        return;
                    }
                    ModLogger.debug("[FAST-CACHE] Affichage reussi - Message original peut etre supprime");
                    completeSuccess(future);
                } catch (Exception e) {
                    ModLogger.error("[FAST-CACHE] Echec d'affichage du hit cache", e);
                    ThreadSafeMessageService.sendError("translation.error.processing");
                    TranslationAsyncErrorHandler.failFuture(future, e);
                } finally {
                    translationService.setTranslationInProgress(false);
                }
            });
            if (!scheduled) {
                failMainThreadUnavailable(translationService, future);
            }
            return true;
        } catch (Exception e) {
            ModLogger.debug("[FAST-CACHE] Indisponible, passage au flux complet: {}",
                    e.getMessage());
            return false;
        }
    }

    public static void execute(String sender, String text, TranslationContext context,
                               boolean isTargetedPlayer, boolean isTranslationCommand,
                               String privateTarget,
                               ConfigurationManager configurationManager,
                               TranslationService translationService,
                               CompletableFuture<Void> future) {
        try {
            ModLogger.debug("processTranslationRequest() demarre");

            TranslationCache cache = TranslationCache.getInstance();
            translationService.setTranslationInProgress(true);

            String sourceLang = context.getSourceLanguageCode();
            String preparedText = context.getPreparedText();
            String finalTargetLangCode = context.getTargetLanguageCode();
            String currentEngine = configurationManager.getCurrentEngine();
            String modelId = configurationManager.getModelForEngine(currentEngine);
            String cacheKey = cache.getCacheKey(preparedText, sourceLang, finalTargetLangCode,
                    modelId);

            ModLogger.debug("Recherche dans le cache...");
            String cachedResult = cache.getCachedTranslationByKey(cacheKey);
            if (cachedResult != null) {
                cache.recordCacheHit();
                ModLogger.debug("Cache hit pour: '{}'", text);
                updateLastTranslationState(cache, text, finalTargetLangCode, cacheKey,
                        canUseTrsClear(isTargetedPlayer));

                // Le cache unifie (texte + cible) est alimente lors du succes API
                // (voir plus bas), en meme temps que le cache contextuel. Inutile de
                // le reecrire a chaque hit : l'entree existe deja pour le fast-path.

                boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
                    try {
                        if (!TranslationRenderer.renderTranslation(sender, text, cachedResult,
                                isTargetedPlayer, isTranslationCommand, privateTarget)) {
                            failRender(translationService, future);
                            return;
                        }
                        ModLogger.debug("CACHE HIT REUSSI - Message original peut etre supprime");
                        completeSuccess(future);
                    } catch (Exception e) {
                        ModLogger.error("ECHEC CACHE HIT - Message original conserve", e);
                        ThreadSafeMessageService.sendError("translation.error.processing");
                        TranslationAsyncErrorHandler.failFuture(future, e);
                    } finally {
                        translationService.setTranslationInProgress(false);
                    }
                });
                if (!scheduled) {
                    failMainThreadUnavailable(translationService, future);
                }
                return;
            }

            cache.recordCacheMiss();
            ModLogger.debug("Appel API pour: '{}' ({}->{})", text, sourceLang, finalTargetLangCode);

            if (shouldShowLoadingMessage(isTargetedPlayer, isTranslationCommand, privateTarget)) {
                ThreadSafeMessageService.sendInfo("translation.loading");
            }

            RetryPolicy.withRetryAsync(() -> AbstractAIEngine.translateMessageWithSourceLangUncached(
                    preparedText,
                    sourceLang,
                    finalTargetLangCode,
                    currentEngine
            ), 30, TimeUnit.SECONDS).thenAccept(result -> {
                if (result != null && !result.isEmpty()) {
                    cache.cacheTranslationByKey(cacheKey, result);
                    // Indexe aussi sous la cle unifiee (texte brut + cible) pour le fast-path.
                    cache.cacheTranslation(text, finalTargetLangCode, result);
                    updateLastTranslationState(cache, text, finalTargetLangCode, cacheKey,
                            canUseTrsClear(isTargetedPlayer));
                    ModLogger.debug("Traduction API recue: '{}' -> '{}'", preparedText, result);

                    boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
                        try {
                            if (!TranslationRenderer.renderTranslation(sender, text, result,
                                    isTargetedPlayer, isTranslationCommand, privateTarget)) {
                                failRender(translationService, future);
                                return;
                            }
                            ModLogger.debug("TRADUCTION REUSSIE - Message original peut etre supprime");
                            completeSuccess(future);
                        } catch (Exception e) {
                            ModLogger.error("ECHEC TRAITEMENT TRADUCTION - Message original conserve",
                                    e);
                            TranslationAsyncErrorHandler.handleManagedError(e, ErrorType.TRANSLATION);
                            TranslationAsyncErrorHandler.failFuture(future, e);
                        } finally {
                            translationService.setTranslationInProgress(false);
                        }
                    });
                    if (!scheduled) {
                        failMainThreadUnavailable(translationService, future);
                    }
                } else {
                    ModLogger.error("Resultat de traduction vide");
                    TranslationAsyncErrorHandler.failWithMessage(translationService, future,
                            "translation.error.empty_result",
                            new IllegalStateException("Empty translation result"));
                }
            }).exceptionally(throwable -> {
                ModLogger.error("ECHEC TRADUCTION API - Message original conserve", throwable);
                TranslationAsyncErrorHandler.failAndReset(translationService, future, throwable,
                        ErrorType.API);
                return null;
            });
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la traduction dans processTranslationRequest", e);
            TranslationAsyncErrorHandler.failAndReset(translationService, future, e,
                    ErrorType.TRANSLATION);
        }
    }

    private static void updateLastTranslationState(TranslationCache cache, String text,
                                                   String targetLanguageCode, String cacheKey,
                                                   boolean trsClearAllowed) {
        if (!trsClearAllowed) {
            return;
        }

        cache.setLastTranslatedText(text);
        cache.setLastTargetLanguage(targetLanguageCode);
        cache.setLastCacheKey(cacheKey);
        cache.setLastTranslationTimestamp(System.currentTimeMillis());
        cache.setTrsClearCommandUsed(false);
    }

    static boolean canUseTrsClear(boolean isTargetedPlayer) {
        return !isTargetedPlayer;
    }

    static boolean shouldShowLoadingMessage(boolean isTargetedPlayer,
                                            boolean isTranslationCommand,
                                            String privateTarget) {
        return !isTargetedPlayer || isTranslationCommand
                || (privateTarget != null && !privateTarget.trim().isEmpty());
    }

    private static void completeSuccess(CompletableFuture<Void> future) {
        if (future != null && !future.isDone()) {
            future.complete(null);
        }
    }

    private static void failMainThreadUnavailable(TranslationService translationService,
                                                  CompletableFuture<Void> future) {
        IllegalStateException error =
                new IllegalStateException("Minecraft main thread unavailable");
        translationService.setTranslationInProgress(false);
        TranslationAsyncErrorHandler.failFuture(future, error);
    }

    private static void failRender(TranslationService translationService,
                                   CompletableFuture<Void> future) {
        ThreadSafeMessageService.sendError("translation.error.processing");
        TranslationAsyncErrorHandler.failFuture(future,
                new IllegalStateException("Translation render failed"));
    }
}
