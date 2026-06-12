package fr.ntgitg.mineglot.core.service.chat;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ChatCleanupService {

    private volatile boolean running;
    private volatile ScheduledFuture<?> cleanupTask;

    private ChatCleanupService() {
        this.running = false;
        this.cleanupTask = null;
    }

    public static ChatCleanupService getInstance() {
        return SingletonManager.getInstance(ChatCleanupService.class, ChatCleanupService::new);
    }

    public synchronized void start() {
        if (running) {
            ModLogger.warn("Le service de nettoyage est deja actif");
            return;
        }

        cleanupTask = ThreadManager.getFeatureExecutor().scheduleAtFixedRate(() -> {
            try {
                ChatMessageManager.getInstance().cleanupOldMessages();
            } catch (Exception e) {
                ModLogger.error("Erreur lors du nettoyage automatique des messages", e);
            }
        }, 2, 2, TimeUnit.MINUTES);

        running = true;
        ModLogger.info("Service de nettoyage automatique des messages demarre");
    }

    public synchronized void stop() {
        if (!running) {
            ModLogger.warn("Le service de nettoyage n'est pas actif");
            return;
        }

        if (cleanupTask != null) {
            cleanupTask.cancel(false);
            cleanupTask = null;
        }

        running = false;
        ModLogger.info("Service de nettoyage automatique des messages arrete");
    }

    public boolean isRunning() {
        return running;
    }

    public void forceCleanup() {
        try {
            ChatMessageManager.getInstance().forceCleanupAllMessages();
            ModLogger.info("Nettoyage force des messages effectue");
        } catch (Exception e) {
            ModLogger.error("Erreur lors du nettoyage force des messages", e);
        }
    }

    public int getPendingMessageCount() {
        return ChatMessageManager.getInstance().getPendingMessageCount();
    }
}
