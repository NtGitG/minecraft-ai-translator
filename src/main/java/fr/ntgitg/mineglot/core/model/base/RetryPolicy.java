package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public final class RetryPolicy {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000;
    private static final long MAX_BACKOFF_MS = 30_000;

    private RetryPolicy() {
    }

    public static <T> CompletableFuture<T> withRetryAsync(RetryableOperation<T> operation) {
        CompletableFuture<T> future = new CompletableFuture<>();
        submitAttempt(operation, future, new AtomicReference<>(), 1, INITIAL_BACKOFF_MS);
        return future;
    }

    public static <T> CompletableFuture<T> withRetryAsync(RetryableOperation<T> operation,
                                                          long timeout,
                                                          TimeUnit timeUnit) {
        CompletableFuture<T> future = new CompletableFuture<>();
        AtomicReference<java.util.concurrent.Future<?>> taskFuture = new AtomicReference<>();

        ScheduledFuture<?> timeoutFuture = ThreadManager.getRetryExecutor().schedule(() -> {
            if (!future.isDone()) {
                ModLogger.warn("Timeout apres {} {} - annulation de la requete et de la tache",
                        timeout, timeUnit);

                java.util.concurrent.Future<?> currentTask = taskFuture.get();
                if (currentTask != null && !currentTask.isDone()) {
                    currentTask.cancel(true);
                    ModLogger.debug("Tache en arriere-plan annulee");
                }

                future.completeExceptionally(
                        new TimeoutException("Retry timeout after " + timeout + " " + timeUnit));
            }
        }, timeout, timeUnit);

        future.whenComplete((result, error) -> timeoutFuture.cancel(false));
        submitAttempt(operation, future, taskFuture, 1, INITIAL_BACKOFF_MS);

        return future;
    }

    private static <T> void submitAttempt(RetryableOperation<T> operation,
                                          CompletableFuture<T> future,
                                          AtomicReference<java.util.concurrent.Future<?>> taskFuture,
                                          int attempt,
                                          long backoffMs) {
        if (future.isDone()) {
            return;
        }

        try {
            java.util.concurrent.Future<?> submitted = ThreadManager.getHttpExecutor().submit(() ->
                    runAttempt(operation, future, taskFuture, attempt, backoffMs));
            taskFuture.set(submitted);
        } catch (RuntimeException e) {
            if (!future.isDone()) {
                future.completeExceptionally(new IOException("Executor HTTP indisponible", e));
            }
        }
    }

    private static <T> void runAttempt(RetryableOperation<T> operation,
                                       CompletableFuture<T> future,
                                       AtomicReference<java.util.concurrent.Future<?>> taskFuture,
                                       int attempt,
                                       long backoffMs) {
        if (future.isDone()) {
            return;
        }

        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Tache annulee par timeout");
            }

            T result = operation.execute();
            if (!future.isDone()) {
                future.complete(result);
            }
        } catch (IOException ioe) {
            handleAsyncIOException(operation, future, taskFuture, attempt, backoffMs, ioe);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!future.isDone()) {
                future.completeExceptionally(new TimeoutException("Tache annulee par timeout"));
            }
        } catch (Exception e) {
            if (!future.isDone()) {
                future.completeExceptionally(new IOException("Erreur inattendue", e));
            }
        }
    }

    private static <T> void handleAsyncIOException(RetryableOperation<T> operation,
                                                   CompletableFuture<T> future,
                                                   AtomicReference<java.util.concurrent.Future<?>> taskFuture,
                                                   int attempt,
                                                   long backoffMs,
                                                   IOException error) {
        if (future.isDone()) {
            return;
        }

        if (attempt >= MAX_RETRIES || !shouldRetry(error)) {
            future.completeExceptionally(new IOException(
                    "Operation echouee apres " + attempt + " tentatives", error));
            return;
        }

        long delayMs = backoffMs + jitter(backoffMs);
        long nextBackoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);

        ModLogger.warn("Retry {}/{} echoue: {} - prochaine tentative dans {}ms",
                attempt, MAX_RETRIES, getDetailedErrorMessage(error), delayMs);

        try {
            ThreadManager.getRetryExecutor().schedule(() ->
                    submitAttempt(operation, future, taskFuture, attempt + 1, nextBackoffMs),
                    delayMs, TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            if (!future.isDone()) {
                future.completeExceptionally(new IOException("Planification retry impossible", e));
            }
        }
    }

    private static long jitter(long backoff) {
        return ThreadLocalRandom.current().nextLong(backoff);
    }

    private static boolean shouldRetry(IOException ioe) {
        if (!(ioe instanceof BaseHttpClient.ApiHttpException)) {
            return true;
        }
        int code = ((BaseHttpClient.ApiHttpException) ioe).getStatusCode();
        return code == 429 || (code >= 500 && code < 600);
    }

    private static String getDetailedErrorMessage(IOException ioe) {
        if (ioe instanceof BaseHttpClient.ApiHttpException) {
            BaseHttpClient.ApiHttpException apiEx = (BaseHttpClient.ApiHttpException) ioe;
            int statusCode = apiEx.getStatusCode();

            switch (statusCode) {
                case 429:
                    return "Rate limit atteint (429)";
                case 500:
                    return "Erreur serveur interne (500)";
                case 502:
                    return "Bad Gateway (502)";
                case 503:
                    return "Service Unavailable (503)";
                case 504:
                    return "Gateway Timeout (504)";
                default:
                    return String.format("Erreur HTTP %d: %s", statusCode, ioe.getMessage());
            }
        }

        String message = ioe.getMessage();
        if (message == null) {
            return ioe.getClass().getSimpleName();
        }

        if (message.contains("Connection") || message.contains("connect")) {
            return "Erreur de connexion reseau";
        }

        if (message.contains("timeout") || message.contains("Timeout")) {
            return "Timeout de connexion";
        }

        return message;
    }

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws IOException;
    }
}
