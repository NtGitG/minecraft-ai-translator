package fr.ntgitg.mineglot.core.service;

public interface Service {

    void start();

    void stop();

    boolean isOperational();

    String getName();

    default void clearCache() {
    }

    default long getCacheSize() {
        return 0;
    }

    default void shutdown() {
        stop();
    }
}
