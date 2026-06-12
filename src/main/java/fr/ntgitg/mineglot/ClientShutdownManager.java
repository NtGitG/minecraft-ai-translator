package fr.ntgitg.mineglot;

import fr.ntgitg.mineglot.core.model.base.BaseHttpClient;
import fr.ntgitg.mineglot.core.service.ServiceManager;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.features.signs.SignTranslationHandler;
import fr.ntgitg.mineglot.monitoring.metrics.MetricsManager;
import fr.ntgitg.mineglot.ui.hud.manager.HUDManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates shutdown and lightweight disconnect cleanup.
 */
public final class ClientShutdownManager implements AutoCloseable {
    private static final long SHUTDOWN_HOOK_TIMEOUT_SECONDS = 5L;

    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean shutdownComplete = new AtomicBoolean(false);
    private final AtomicBoolean shutdownHookInstalled = new AtomicBoolean(false);
    private final List<Runnable> shutdownTasks = new CopyOnWriteArrayList<>();
    private final List<Runnable> essentialTasks = new CopyOnWriteArrayList<>();
    private final Object stateLock = new Object();

    private volatile CompletableFuture<Void> shutdownFuture;

    private ClientShutdownManager() {
        initializeShutdownTasks();
        initializeEssentialTasks();
    }

    public static ClientShutdownManager getInstance() {
        return SingletonManager.getInstance(ClientShutdownManager.class, ClientShutdownManager::new);
    }

    public void addShutdownTask(Runnable task) {
        if (task != null) {
            shutdownTasks.add(task);
        }
    }

    public void addEssentialTask(Runnable task) {
        if (task != null) {
            essentialTasks.add(task);
        }
    }

    public CompletableFuture<Void> shutdown() {
        synchronized (stateLock) {
            if (shutdownFuture != null && !shutdownFuture.isDone()) {
                return shutdownFuture;
            }

            if (shutdownComplete.get()) {
                return shutdownFuture != null ? shutdownFuture : CompletableFuture.completedFuture(null);
            }

            if (!shutdownInProgress.compareAndSet(false, true)) {
                return shutdownFuture != null ? shutdownFuture : CompletableFuture.completedFuture(null);
            }

            ModLogger.info("=== FERMETURE DU MOD ===");
            shutdownComplete.set(false);

            shutdownFuture = CompletableFuture.runAsync(this::runShutdownInternal,
                    ClientShutdownManager::runShutdownTask)
                    .whenComplete((unused, error) -> {
                        shutdownInProgress.set(false);
                        shutdownComplete.set(error == null);

                        if (error != null) {
                            ModLogger.error("Shutdown termine avec erreur", error);
                        } else {
                            ModLogger.info("Shutdown termine avec succes");
                        }
                    });

            return shutdownFuture;
        }
    }

    public CompletableFuture<Void> performEssentialTasks() {
        if (ThreadManager.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }
        return ThreadManager.runAsync(() -> executeTasks(essentialTasks, "TACHES ESSENTIELLES"));
    }

    public CompletableFuture<Void> handleDisconnection() {
        ModLogger.info("Deconnexion detectee - nettoyage des donnees serveur");
        return performEssentialTasks();
    }

    public void installShutdownHook() {
        if (!shutdownHookInstalled.compareAndSet(false, true)) {
            return;
        }

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ModLogger.info("=== HOOK DE SHUTDOWN MineGlot ACTIVE ===");
                try {
                    shutdown().get(SHUTDOWN_HOOK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    ModLogger.info("Shutdown du mod MineGlot termine avec succes");
                } catch (Exception e) {
                    ModLogger.error(
                            "Erreur lors du shutdown observable, fallback au shutdown synchrone",
                            e);
                    try {
                        closeAndWait();
                        ModLogger.info("Shutdown synchrone reussi");
                    } catch (Exception fallbackEx) {
                        ModLogger.error("Erreur critique lors du shutdown synchrone", fallbackEx);
                        ModLogger.warn("Le mod MineGlot n'a pas pu s'arreter proprement");
                    }
                }
            }, "MineGlot-ShutdownHook"));
            ModLogger.info("Hook de shutdown MineGlot configure avec succes");
        } catch (Exception e) {
            shutdownHookInstalled.set(false);
            ModLogger.error("Erreur lors de la configuration du hook de shutdown", e);
            ModLogger.warn("Le mod MineGlot ne pourra pas s'arreter proprement");
        }
    }

    public boolean isShuttingDown() {
        return shutdownInProgress.get();
    }

    public boolean isShutdownComplete() {
        return shutdownComplete.get();
    }

    public CompletableFuture<Void> getShutdownFuture() {
        return shutdownFuture;
    }

    @Override
    public void close() {
        closeAndWait();
    }

    public void closeAndWait() {
        try {
            shutdown().get(SHUTDOWN_HOOK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interruption pendant le shutdown", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout pendant le shutdown", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Erreur lors du shutdown", cause);
        }
    }

    private void runShutdownInternal() {
        long startTime = System.currentTimeMillis();
        Throwable firstError = null;

        try {
            executeTasks(shutdownTasks, "ARRET DES COMPOSANTS");
        } catch (Throwable t) {
            firstError = t;
        }

        try {
            ThreadManager.shutdown();
        } catch (Throwable t) {
            if (firstError == null) {
                firstError = t;
            } else {
                firstError.addSuppressed(t);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        ModLogger.info("=== FERMETURE TERMINEE EN {}ms ===", duration);

        if (firstError != null) {
            throw new IllegalStateException("Erreur pendant le shutdown", firstError);
        }
    }

    private static void runShutdownTask(Runnable task) {
        Thread thread = new Thread(task, "MineGlot-Shutdown");
        thread.setDaemon(true);
        thread.start();
    }

    private void executeTasks(List<Runnable> tasks, String phase) {
        ModLogger.info("=== {} ===", phase);

        int successCount = 0;
        int errorCount = 0;
        Throwable firstError = null;

        for (int i = 0; i < tasks.size(); i++) {
            Runnable task = tasks.get(i);
            try {
                task.run();
                successCount++;
            } catch (Throwable t) {
                errorCount++;
                if (firstError == null) {
                    firstError = t;
                }
                ModLogger.error("Erreur dans la tache {}/{} de '{}'", i + 1, tasks.size(), phase, t);
            }
        }

        ModLogger.info("{} termine : {} reussies, {} erreurs", phase, successCount, errorCount);

        if (firstError != null) {
            throw new IllegalStateException("Echec pendant la phase: " + phase, firstError);
        }
    }

    private void initializeShutdownTasks() {
        addShutdownTask(() -> {
            ModLogger.info("Fermeture du client HTTP partage...");
            BaseHttpClient.shutdownSharedClient();
            ModLogger.info("Client HTTP partage ferme");
        });

        addShutdownTask(() -> {
            ModLogger.info("Arret des services...");
            ServiceManager.getInstance().stopServices();
            ModLogger.info("Services arretes avec succes");
        });

        addShutdownTask(() -> {
            ModLogger.info("Nettoyage du HUD...");
            HUDManager.getInstance().cleanup();
            ModLogger.info("HUD nettoye avec succes");
        });

        addShutdownTask(() -> {
            ModLogger.info("Arret des handlers de traduction...");
            SignTranslationHandler.getInstance().shutdown();
            ModLogger.info("Handlers de traduction arretes");
        });

        addShutdownTask(() -> {
            ModLogger.info("Sauvegarde finale des metriques...");
            MetricsManager.getInstance().flush();
            ModLogger.info("Metriques sauvegardees");
        });

        addShutdownTask(() -> {
            ModLogger.info("Arret des services asynchrones locaux...");
            fr.ntgitg.mineglot.core.service.chat.ChatCleanupService.getInstance().stop();
            ModLogger.info("Services asynchrones locaux arretes");
        });
    }

    private void initializeEssentialTasks() {
        addEssentialTask(() -> {
            fr.ntgitg.mineglot.core.command.target.services.TargetPlayerList.getInstance().clear();
            runOnClientThread(() -> fr.ntgitg.mineglot.core.command.translate.translate_chat.services.ChatSelectionService
                    .getInstance().setSelecting(false, false));
            ModLogger.info("Liste des joueurs cibles nettoyee");
        });
    }

    private void runOnClientThread(Runnable task) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            task.run();
            return;
        }

        if (minecraft.isCallingFromMinecraftThread()) {
            task.run();
            return;
        }

        minecraft.addScheduledTask(task);
    }
}
