package fr.ntgitg.mineglot.core.translation;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.AbstractService;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public final class TranslationService extends AbstractService {

    private final ConfigurationManager configManager;

    private final AtomicInteger translationsInFlight = new AtomicInteger();

    private TranslationService() {
        super("Translation");
        this.configManager = ConfigurationManager.getInstance();
        resetTranslationState();
    }

    public static TranslationService getInstance() {
        return SingletonManager.getInstance(TranslationService.class, TranslationService::new);
    }

    @Override
    protected synchronized void doStart() {
        configManager.getConfig();

        ExecutorService executor = ThreadManager.getTranslationExecutor();
        if (executor == null || executor.isShutdown()) {
            throw new IllegalStateException("Translation executor is not available");
        }
    }

    @Override
    protected synchronized void doStop() {
        resetTranslationState();
        ModLogger.info("Arret du service de traduction");
    }

    public boolean submitTranslation(Runnable task) {
        if (task == null) {
            ModLogger.warn("Tentative de soumission d'une tache de traduction nulle");
            return false;
        }

        if (!isOperational()) {
            ModLogger.debug("Le service de traduction n'est pas operationnel");
            return false;
        }

        ExecutorService executor = ThreadManager.getTranslationExecutor();
        if (executor == null || executor.isShutdown()) {
            ModLogger.warn("Executor de traduction indisponible");
            return false;
        }

        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                ModLogger.error("Echec traduction - Service: {}, Thread: {}",
                        isOperational() ? "operationnel" : "arrete",
                        Thread.currentThread().getName(),
                        e);
            }
        });
        return true;
    }

    public synchronized void reset() {
        ModLogger.debug("Reinitialisation du service de traduction...");
        stop();
        start();
        ModLogger.debug("Service de traduction reinitialise");
    }

    public void setTranslationInProgress(boolean inProgress) {
        if (inProgress) {
            translationsInFlight.incrementAndGet();
            return;
        }

        translationsInFlight.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    public boolean isTranslationInProgress() {
        return translationsInFlight.get() > 0;
    }

    public void resetTranslationState() {
        translationsInFlight.set(0);
    }
}
