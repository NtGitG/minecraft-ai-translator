package fr.ntgitg.mineglot.ui.gui.screens.update;

import fr.ntgitg.mineglot.MineGlot;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.update.ReleaseInfo;
import fr.ntgitg.mineglot.core.update.UpdateCheckAccess;
import fr.ntgitg.mineglot.core.update.UpdateCheckResult;
import fr.ntgitg.mineglot.core.update.UpdateCheckService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateNotificationController {
    private final UpdateCheckAccess updateCheckService;
    private final Executor resultExecutor;
    private final UpdateClientBridge clientBridge;
    private final AtomicBoolean resultListenerRegistered = new AtomicBoolean(false);
    private final AtomicBoolean promptScheduled = new AtomicBoolean(false);

    private volatile GuiScreen latestMainMenu;
    private volatile ReleaseInfo availableRelease;

    private UpdateNotificationController() {
        this(UpdateCheckService.getInstance(), ThreadManager.getFeatureExecutor(),
                new MinecraftUpdateClientBridge());
    }

    UpdateNotificationController(UpdateCheckAccess updateCheckService, Executor resultExecutor,
                                 UpdateClientBridge clientBridge) {
        if (updateCheckService == null || resultExecutor == null || clientBridge == null) {
            throw new IllegalArgumentException("Update notification dependencies cannot be null");
        }
        this.updateCheckService = updateCheckService;
        this.resultExecutor = resultExecutor;
        this.clientBridge = clientBridge;
    }

    public static UpdateNotificationController getInstance() {
        return SingletonManager.getInstance(UpdateNotificationController.class,
                UpdateNotificationController::new);
    }

    public void onMainMenuOpened(GuiScreen parentScreen) {
        if (parentScreen == null) {
            return;
        }

        latestMainMenu = parentScreen;
        if (updateCheckService.isDismissedForSession()) {
            return;
        }

        ReleaseInfo cachedRelease = availableRelease;
        if (cachedRelease != null) {
            scheduleCachedPrompt(cachedRelease);
            return;
        }

        if (!resultListenerRegistered.compareAndSet(false, true)) {
            return;
        }

        try {
            updateCheckService.checkOnceAsync().thenAcceptAsync(result -> {
                if (result != null
                        && result.getStatus() == UpdateCheckResult.Status.UPDATE_AVAILABLE
                        && result.getReleaseInfo() != null) {
                    availableRelease = result.getReleaseInfo();
                    schedulePrompt(result.getReleaseInfo());
                }
            }, resultExecutor).exceptionally(error -> {
                ModLogger.debug("Notification de mise a jour ignoree: {}",
                        error.getClass().getSimpleName());
                return null;
            });
        } catch (RuntimeException e) {
            ModLogger.debug("Notification de mise a jour non disponible: {}", e.getMessage());
        }
    }

    private void scheduleCachedPrompt(ReleaseInfo releaseInfo) {
        try {
            resultExecutor.execute(() -> schedulePrompt(releaseInfo));
        } catch (RuntimeException e) {
            ModLogger.debug("Notification en cache non planifiee: {}", e.getMessage());
        }
    }

    private void schedulePrompt(ReleaseInfo releaseInfo) {
        if (!promptScheduled.compareAndSet(false, true)) {
            return;
        }

        try {
            clientBridge.runOnClientThread(() -> {
                GuiScreen parentScreen = latestMainMenu;
                if (updateCheckService.isDismissedForSession()
                        || parentScreen == null
                        || clientBridge.getCurrentScreen() != parentScreen) {
                    promptScheduled.set(false);
                    return;
                }

                try {
                    clientBridge.showPrompt(parentScreen, releaseInfo, MineGlot.VERSION,
                            updateCheckService::dismissForSession);
                } catch (RuntimeException e) {
                    promptScheduled.set(false);
                    ModLogger.warn("Impossible d'afficher la notification de mise a jour: {}",
                            e.getMessage());
                }
            });
        } catch (RuntimeException e) {
            promptScheduled.set(false);
            ModLogger.debug("Notification de mise a jour non planifiee: {}", e.getMessage());
        }
    }

    private static final class MinecraftUpdateClientBridge implements UpdateClientBridge {
        @Override
        public void runOnClientThread(Runnable task) {
            Minecraft.getMinecraft().addScheduledTask(task);
        }

        @Override
        public GuiScreen getCurrentScreen() {
            return Minecraft.getMinecraft().currentScreen;
        }

        @Override
        public void showPrompt(GuiScreen parentScreen, ReleaseInfo releaseInfo,
                               String currentVersion, Runnable dismissAction) {
            Minecraft.getMinecraft().displayGuiScreen(new UpdateAvailableGui(parentScreen,
                    releaseInfo, currentVersion, dismissAction));
        }
    }
}
