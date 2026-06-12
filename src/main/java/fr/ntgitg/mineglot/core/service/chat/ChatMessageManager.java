package fr.ntgitg.mineglot.core.service.chat;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChatMessageManager {

    private final ConcurrentMap<String, ChatMessageInfo> pendingMessages =
            new ConcurrentHashMap<>();
    private final AtomicInteger messageCounter = new AtomicInteger(0);

    private ChatMessageManager() {
    }

    public static ChatMessageManager getInstance() {
        return SingletonManager.getInstance(ChatMessageManager.class, ChatMessageManager::new);
    }

    public PendingMessage registerPendingMessage(String originalMessage, String sender) {
        int counter = messageCounter.incrementAndGet();
        String messageId = generateMessageId(counter);
        int chatLineId = -counter;
        ChatMessageInfo info =
                new ChatMessageInfo(sender, System.currentTimeMillis(), chatLineId);
        pendingMessages.put(messageId, info);

        ModLogger.debug("Message enregistre pour suppression: ID={}, sender={}, message={}", messageId,
                sender, originalMessage);

        return new PendingMessage(messageId, chatLineId);
    }

    public void displayOriginalMessage(IChatComponent message, int chatLineId) {
        if (message == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.ingameGUI == null) {
            return;
        }
        GuiNewChat chat = mc.ingameGUI.getChatGUI();
        chat.printChatMessageWithOptionalDeletion(message, chatLineId);
    }

    public boolean removePendingMessage(String messageId) {
        ChatMessageInfo info = pendingMessages.remove(messageId);
        if (info == null) {
            ModLogger.warn("Tentative de suppression d'un message inexistant: {}", messageId);
            return false;
        }

        boolean scheduled = ThreadSafeMessageService.scheduleOnMainThread(() -> {
            try {
                removeMessageFromChat(info.chatLineId);
                ModLogger.debug("Message supprime du chat: ID={}, sender={}", messageId, info.sender);
            } catch (Exception e) {
                ModLogger.error("Erreur lors de la suppression du message du chat: ID={}", messageId,
                        e);
            }
        });

        return scheduled;
    }

    public void releasePendingMessage(String messageId) {
        pendingMessages.remove(messageId);
    }

    private void removeMessageFromChat(int chatLineId) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.ingameGUI != null) {
                mc.ingameGUI.getChatGUI().deleteChatLine(chatLineId);
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la suppression du message du chat", e);
        }
    }

    public void cleanupOldMessages() {
        long cutoffTime = System.currentTimeMillis() - (5 * 60 * 1000);
        final AtomicInteger removedCount = new AtomicInteger(0);

        pendingMessages.entrySet().removeIf(entry -> {
            if (entry.getValue().timestamp < cutoffTime) {
                ModLogger.debug("Suppression du message expire: ID={}", entry.getKey());
                removedCount.incrementAndGet();
                return true;
            }
            return false;
        });

        int finalCount = removedCount.get();
        if (finalCount > 0) {
            ModLogger.info("Nettoyage automatique: {} messages expires supprimes", finalCount);
        }
    }

    public int getPendingMessageCount() {
        return pendingMessages.size();
    }

    public void forceCleanupAllMessages() {
        int count = pendingMessages.size();
        pendingMessages.clear();
        ModLogger.info("Nettoyage force: {} messages en attente supprimes", count);
    }

    private String generateMessageId(int counter) {
        return "msg_" + System.currentTimeMillis() + "_" + counter;
    }

    public static final class PendingMessage {
        private final String id;
        private final int chatLineId;

        private PendingMessage(String id, int chatLineId) {
            this.id = id;
            this.chatLineId = chatLineId;
        }

        public String getId() {
            return id;
        }

        public int getChatLineId() {
            return chatLineId;
        }
    }

    private static class ChatMessageInfo {
        final String sender;
        final long timestamp;
        final int chatLineId;

        ChatMessageInfo(String sender, long timestamp, int chatLineId) {
            this.sender = sender;
            this.timestamp = timestamp;
            this.chatLineId = chatLineId;
        }
    }
}
