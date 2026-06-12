package fr.ntgitg.mineglot.core.chat;

import fr.ntgitg.mineglot.core.command.target.services.TargetPlayerList;
import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.utils.extractor.PlayerNameExtractor;

import java.util.UUID;

public class ChatTranslationFilter {

    public static boolean canTranslateAnyChatMessage() {
        return TargetPlayerList.getInstance().size() > 0;
    }

    public static boolean needsTranslation(String pseudo, String message, String rawMessage) {
        try {
            net.minecraft.client.entity.EntityPlayerSP self =
                    net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            if (self != null && self.getName().equals(pseudo)) {
                return false;
            }
        } catch (Exception ignored) {
        }

        UUID playerId = PlayerNameManager.getInstance().getPlayerUuidByName(pseudo);
        boolean isTarget = playerId != null && TargetPlayerList.getInstance().contains(playerId);

        if (!isTarget) {
            return false;
        }

        if (!matchesTargetedChatShape(rawMessage, pseudo, message)) {
            return false;
        }

        if (isLikelyPlayerNameMessage(message)) {
            return false;
        }

        return true;
    }

    public static boolean needsTranslation(String pseudo, String message) {
        return needsTranslation(pseudo, message, message);
    }

    static boolean matchesTargetedChatShape(String rawMessage, String pseudo, String message) {
        return isPrivateMessage(rawMessage) || isLikelyPlayerChat(rawMessage, pseudo, message);
    }

    static boolean isPrivateMessage(String message) {
        if (message == null) {
            return false;
        }

        return message.startsWith("From ") || // Essentials
                message.contains(" tells you: ") || // Vanilla
                message.contains(" whispers: ") || // Certains plugins
                message.contains(" vous dit: ") || // Serveurs francais
                message.contains(" -> ") || // Format arrows
                message.startsWith("[MP]") || // Messages prives
                message.startsWith("PM from ") || // Autre format
                message.startsWith("[PRIVATE]"); // Encore un autre
    }

    static boolean isLikelyPlayerChat(String rawMessage, String pseudo, String message) {
        if (rawMessage == null || pseudo == null || pseudo.isEmpty()) {
            return false;
        }

        int idx = rawMessage.indexOf(pseudo);
        if (idx >= 0) {
            String after = rawMessage.substring(idx + pseudo.length());
            int i = 0;
            while (i < after.length() && Character.isWhitespace(after.charAt(i))) {
                i++;
            }
            if (i >= after.length()) {
                return false;
            }

            char sep = after.charAt(i);
            if (sep != ':' && sep != '>' && sep != '\u00BB') {
                return false;
            }

            i++;
            while (i < after.length() && Character.isWhitespace(after.charAt(i))) {
                i++;
            }
            return i < after.length();
        }

        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        int msgIdx = rawMessage.indexOf(message);
        if (msgIdx < 0) {
            return false;
        }

        int sepIdx = Math.max(rawMessage.lastIndexOf(':', msgIdx),
                Math.max(rawMessage.lastIndexOf('>', msgIdx), rawMessage.lastIndexOf('\u00BB', msgIdx)));
        if (sepIdx < 0) {
            return false;
        }

        String between = rawMessage.substring(sepIdx + 1, msgIdx).trim();
        return between.isEmpty();
    }

    private static boolean isLikelyPlayerNameMessage(String message) {
        if (message == null) {
            return false;
        }

        String clean = PlayerNameExtractor.extractBaseName(message);
        if (clean.isEmpty()) {
            return false;
        }

        return PlayerNameManager.getInstance().isPlayerOnline(clean);
    }
}
