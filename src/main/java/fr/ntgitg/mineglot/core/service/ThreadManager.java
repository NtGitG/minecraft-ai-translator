package fr.ntgitg.mineglot.core.service;

import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadManager {

    private final Object lock = new Object();

    private volatile ExecutorService translationExecutor;
    private volatile ExecutorService httpExecutor;
    private volatile ExecutorService dbExecutor;
    private volatile ScheduledExecutorService retryExecutor;
    private volatile ScheduledExecutorService featureExecutor;

    private volatile boolean shutdown;

    private ThreadManager() {
        this.shutdown = false;
    }

    private static ThreadManager getInstance() {
        return SingletonManager.getInstance(ThreadManager.class, ThreadManager::new);
    }

    public static ExecutorService getTranslationExecutor() {
        return getInstance().getTranslationExecutorInternal();
    }

    public static ExecutorService getHttpExecutor() {
        return getInstance().getHttpExecutorInternal();
    }

    public static ExecutorService getDbExecutor() {
        return getInstance().getDbExecutorInternal();
    }

    public static ScheduledExecutorService getRetryExecutor() {
        return getInstance().getRetryExecutorInternal();
    }

    public static ScheduledExecutorService getFeatureExecutor() {
        return getInstance().getFeatureExecutorInternal();
    }

    public static CompletableFuture<Void> runAsync(Runnable task) {
        return getInstance().runAsyncInternal(task, getInstance().getHttpExecutorInternal());
    }

    public static CompletableFuture<Void> runTranslationAsync(Runnable task) {
        return getInstance().runAsyncInternal(task, getInstance().getTranslationExecutorInternal());
    }

    public static CompletableFuture<Void> runDbAsync(Runnable task) {
        return getInstance().runAsyncInternal(task, getInstance().getDbExecutorInternal());
    }

    public static void shutdown() {
        getInstance().shutdownInternal();
    }

    public static boolean isShutdown() {
        return getInstance().shutdown;
    }

    private ExecutorService getTranslationExecutorInternal() {
        ExecutorService current = translationExecutor;
        if (current != null) {
            return current;
        }

        synchronized (lock) {
            if (translationExecutor == null && !shutdown) {
                translationExecutor = Executors.newSingleThreadExecutor(
                        createThreadFactory("Translation"));
            }
            return translationExecutor;
        }
    }

    private ExecutorService getHttpExecutorInternal() {
        ExecutorService current = httpExecutor;
        if (current != null) {
            return current;
        }

        synchronized (lock) {
            if (httpExecutor == null && !shutdown) {
                httpExecutor = Executors.newFixedThreadPool(4, createThreadFactory("HttpClient"));
            }
            return httpExecutor;
        }
    }

    private ExecutorService getDbExecutorInternal() {
        ExecutorService current = dbExecutor;
        if (current != null) {
            return current;
        }

        synchronized (lock) {
            if (dbExecutor == null && !shutdown) {
                dbExecutor = Executors.newSingleThreadExecutor(createThreadFactory("Database"));
            }
            return dbExecutor;
        }
    }

    private ScheduledExecutorService getRetryExecutorInternal() {
        ScheduledExecutorService current = retryExecutor;
        if (current != null) {
            return current;
        }

        synchronized (lock) {
            if (retryExecutor == null && !shutdown) {
                retryExecutor = Executors.newSingleThreadScheduledExecutor(
                        createThreadFactory("Retry"));
            }
            return retryExecutor;
        }
    }

    private ScheduledExecutorService getFeatureExecutorInternal() {
        ScheduledExecutorService current = featureExecutor;
        if (current != null) {
            return current;
        }

        synchronized (lock) {
            if (featureExecutor == null && !shutdown) {
                featureExecutor = Executors.newSingleThreadScheduledExecutor(
                        createThreadFactory("Feature"));
            }
            return featureExecutor;
        }
    }

    private CompletableFuture<Void> runAsyncInternal(Runnable task, ExecutorService executor) {
        if (task == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("task cannot be null"));
            return failed;
        }

        if (executor == null || shutdown || executor.isShutdown()) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("executor is not available"));
            return failed;
        }

        return CompletableFuture.runAsync(task, executor);
    }

    private void shutdownInternal() {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            shutdown = true;
        }

        ModLogger.info("Arret des executors ThreadManager...");

        shutdownExecutor(translationExecutor);
        shutdownExecutor(httpExecutor);
        shutdownExecutor(dbExecutor);
        shutdownExecutor(retryExecutor);
        shutdownExecutor(featureExecutor);

        awaitTermination(translationExecutor);
        awaitTermination(httpExecutor);
        awaitTermination(dbExecutor);
        awaitTermination(retryExecutor);
        awaitTermination(featureExecutor);

        synchronized (lock) {
            translationExecutor = null;
            httpExecutor = null;
            dbExecutor = null;
            retryExecutor = null;
            featureExecutor = null;
        }

        ModLogger.info("ThreadManager arrete proprement");
    }

    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void awaitTermination(ExecutorService executor) {
        if (executor == null) {
            return;
        }

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                ModLogger.warn("Timeout sur l'arret d'un executor, force shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ModLogger.error("Interruption lors de l'arret des executors", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory createThreadFactory(String name) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable,
                    "MineGlot-" + name + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
