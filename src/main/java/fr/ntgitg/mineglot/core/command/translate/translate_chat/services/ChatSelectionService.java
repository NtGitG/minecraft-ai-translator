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
    private volatile long selectionGeneration;
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
        long generation = ++selectionGeneration;
        this.selecting = selecting;

        if (selecting) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.theWorld != null) {
                selectionStartTime = minecraft.theWorld.getTotalWorldTime();
            } else {
                selectionStartTime = 0L;
            }

            startSelectionTimer(generation);

            if (notifyPlayer && minecraft != null && minecraft.thePlayer != null) {
                MessageService.sendInfo(minecraft.thePlayer, "chat.selection_activated");
            }
            return;
        }

        stopSelectionTimer();
        ChatSelectionDecorator.restoreOriginalClicks();
        if (notifyPlayer) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.thePlayer != null) {
                MessageService.sendInfo(minecraft.thePlayer, "chat.selection_deactivated");
            }
        }
    }

    private synchronized void startSelectionTimer(long generation) {
        stopSelectionTimer();

        warningTask = ThreadManager.getFeatureExecutor().schedule(() ->
                ThreadSafeMessageService.scheduleOnMainThread(() -> {
                    if (!isCurrentSelection(generation)) {
                        return;
                    }
                    Minecraft minecraft = Minecraft.getMinecraft();
                    if (minecraft == null || minecraft.thePlayer == null) {
                        return;
                    }
                    MessageService.sendInfo(minecraft.thePlayer, "chat.selection_ending");
                }), 25, TimeUnit.SECONDS);

        shutdownTask = ThreadManager.getFeatureExecutor().schedule(() -> {
            if (!isCurrentSelection(generation)) {
                return;
            }
            boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
                if (isCurrentSelection(generation)) {
                    setSelecting(false);
                }
            });
            if (!scheduled && isCurrentSelection(generation)) {
                setSelecting(false, false);
            }
        }, 30, TimeUnit.SECONDS);
    }

    private boolean isCurrentSelection(long generation) {
        return selecting && selectionGeneration == generation;
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
