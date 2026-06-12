package fr.ntgitg.mineglot.core.service;

import fr.ntgitg.mineglot.utils.log.ModLogger;

public abstract class AbstractService implements Service {

    private volatile boolean isOperational = false;
    private final String serviceName;

    protected AbstractService(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void start() {
        if (!isOperational) {
            try {
                doStart();
                isOperational = true;
            } catch (Exception e) {
                ModLogger.error("Erreur lors du démarrage du service {}", serviceName, e);
                isOperational = false;
                throw new IllegalStateException("Impossible de démarrer le service " + serviceName, e);
            }
        }
    }

    @Override
    public void stop() {
        if (isOperational) {
            try {
                doStop();
                isOperational = false;
                ModLogger.info("Service {} arrêté", serviceName);
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'arrêt du service {}", serviceName, e);
                isOperational = false;
            }
        }
    }

    @Override
    public boolean isOperational() {
        return isOperational;
    }

    @Override
    public String getName() {
        return serviceName;
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop() throws Exception;
}
