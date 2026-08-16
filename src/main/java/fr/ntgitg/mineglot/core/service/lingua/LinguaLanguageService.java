package fr.ntgitg.mineglot.core.service.lingua;

import fr.ntgitg.mineglot.core.service.AbstractService;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LinguaLanguageService extends AbstractService {

    private static final String SERVICE_NAME = "Language";

    private final AtomicBoolean initializing = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private volatile CompletableFuture<Void> initializationFuture;

    private LinguaLanguageService() {
        super(SERVICE_NAME);
    }

    public static LinguaLanguageService getInstance() {
        return SingletonManager.getInstance(LinguaLanguageService.class,
                LinguaLanguageService::new);
    }

    @Override
    protected void doStart() {
        if (initialized.get()) {
            ModLogger.info("LanguageService deja initialise");
            return;
        }

        if (!initializing.compareAndSet(false, true)) {
            ModLogger.warn("LanguageService deja en cours d'initialisation");
            return;
        }

        stopRequested.set(false);
        ModLogger.info("Demarrage de LanguageService - initialisation asynchrone...");

        CompletableFuture<Void> future = new CompletableFuture<>();
        initializationFuture = future;

        Thread thread = new Thread(() -> initializeDetectorOnDedicatedThread(future),
                "MineGlot-LanguageService-Init");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void initializeDetectorOnDedicatedThread(CompletableFuture<Void> future) {
        try {
            ModLogger.info("Initialisation de LanguageDetectorUtil en cours...");
            LinguaDetectorUtil.initialize();

            if (stopRequested.get()) {
                initialized.set(false);
                ModLogger.info("Initialisation LanguageDetectorUtil terminee apres l'arret du service");
            } else {
                initialized.set(true);
                ModLogger.info("LanguageDetectorUtil initialise avec succes (arriere-plan)");
            }

            future.complete(null);
        } catch (Exception e) {
            initialized.set(false);
            ModLogger.error("Erreur lors de l'initialisation de LanguageDetectorUtil", e);
            future.completeExceptionally(
                    new RuntimeException("Echec de l'initialisation de LanguageDetectorUtil", e));
        } finally {
            initializing.set(false);
        }
    }

    @Override
    protected void doStop() {
        ModLogger.info("Arret de LanguageService...");
        stopRequested.set(true);

        CompletableFuture<Void> future = initializationFuture;
        if (future != null && !future.isDone()) {
            try {
                ModLogger.info("Attente de la fin de l'initialisation...");
                future.get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                ModLogger.warn("Timeout lors de l'arret du LanguageService");
            } catch (Exception e) {
                ModLogger.warn("Erreur lors de l'attente de l'initialisation", e);
            }
        }

        initialized.set(false);
        initializing.set(false);
        initializationFuture = null;
        ModLogger.info("LanguageService arrete");
    }

    @Override
    public boolean isOperational() {
        return super.isOperational() && initialized.get();
    }

    public boolean isLanguageDetectorReady() {
        return initialized.get();
    }

    public boolean isInitializing() {
        return initializing.get();
    }
}
