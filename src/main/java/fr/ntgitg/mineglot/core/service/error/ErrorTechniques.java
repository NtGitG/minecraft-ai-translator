package fr.ntgitg.mineglot.core.service.error;

import fr.ntgitg.mineglot.utils.log.ModLogger;

public final class ErrorTechniques {

    private ErrorTechniques() {
    }

    public static void handleAndRethrow(Exception e, String context) {
        logError(e, context);
        throw new RuntimeException("Erreur critique dans " + context, e);
    }

    public static void handleCritical(Exception e, String context) {
        logError(e, context);
        throw new IllegalStateException("Service critique en erreur dans " + context, e);
    }

    public static void handleAndLog(Exception e, String context) {
        logError(e, context);
    }

    private static void logError(Exception e, String context) {
        ModLogger.error("Erreur technique dans {} : {}", context, e.getMessage());
        ModLogger.error("Type d'erreur : {} - Contexte : {}", e.getClass().getSimpleName(), context);

        ModLogger.error("Stack trace complète pour {}", context, e);
    }

    public static void validateNotNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    public static void validateNotEmpty(String str, String name) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
    }

    public static void validateRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
    }

    public static void validatePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public static void validatePositive(double value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public static void validateNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    public static <T> T executeWithErrorHandling(ThrowingSupplier<T> operation, String context) {
        try {
            return operation.get();
        } catch (Exception e) {
            handleAndRethrow(e, context);
            return null; // Ne sera jamais atteint
        }
    }

    public static void executeWithErrorHandling(ThrowingRunnable operation, String context) {
        try {
            operation.run();
        } catch (Exception e) {
            handleAndRethrow(e, context);
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {

        void run() throws Exception;
    }
}
