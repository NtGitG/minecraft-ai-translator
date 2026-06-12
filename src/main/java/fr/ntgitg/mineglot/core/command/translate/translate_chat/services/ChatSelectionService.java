package fr.ntgitg.mineglot.core.command.translate.translate_chat.services;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ChatSelectionService {
    private static final int SELECTION_DURATION = 30 * 20;
    private static final int WARNING_TIME = 5 * 20;

    private volatile boolean selecting;
    private volatile long selectionStartTime;
    private volatile ScheduledFuture<?> warningTask;
    private volatile ScheduledFuture<?> shutdownTask;

    private ChatSelectionService() {
    }

    public static ChatSelectionService getInstance() {
        return SingletonManager.getInstance(ChatSelectionService.class, ChatSelectionService::new);
    }

    public boolean isSelecting() {
        return selecting;
    }

    public synchronized void setSelecting(boolean selecting) {
        setSelecting(selecting, true);
    }

    public synchronized void setSelecting(boolean selecting, boolean notifyPlayer) {
        this.selecting = selecting;

        if (selecting) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.theWorld != null) {
                selectionStartTime = minecraft.theWorld.getTotalWorldTime();
            } else {
                selectionStartTime = 0L;
            }

            startSelectionTimer();

            if (notifyPlayer && minecraft != null && minecraft.thePlayer != null) {
                MessageService.sendInfo(minecraft.thePlayer, "chat.selection_activated");
            }
            return;
        }

        stopSelectionTimer();
        if (notifyPlayer) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.thePlayer != null) {
                MessageService.sendInfo(minecraft.thePlayer, "chat.selection_deactivated");
            }
        }
    }

    private synchronized void startSelectionTimer() {
        stopSelectionTimer();

        warningTask = ThreadManager.getFeatureExecutor().schedule(() ->
                ThreadSafeMessageService.scheduleOnMainThread(() -> {
                    if (!selecting) {
                        return;
                    }
                    Minecraft minecraft = Minecraft.getMinecraft();
                    if (minecraft == null || minecraft.thePlayer == null) {
                        return;
                    }
                    MessageService.sendInfo(minecraft.thePlayer, "chat.selection_ending");
                }), 25, TimeUnit.SECONDS);

        shutdownTask = ThreadManager.getFeatureExecutor().schedule(() -> {
            if (!selecting) {
                return;
            }
            boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
                if (selecting) {
                    setSelecting(false);
                }
            });
            if (!scheduled) {
                setSelecting(false, false);
            }
        }, 30, TimeUnit.SECONDS);
    }

    private synchronized void stopSelectionTimer() {
        if (warningTask != null) {
            warningTask.cancel(false);
            warningTask = null;
        }
        if (shutdownTask != null) {
            shutdownTask.cancel(false);
            shutdownTask = null;
        }
    }

    public long getSelectionStartTime() {
        return selectionStartTime;
    }

    public int getSelectionDuration() {
        return SELECTION_DURATION;
    }

    public int getWarningTime() {
        return WARNING_TIME;
    }
}
