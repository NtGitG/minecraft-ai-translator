package fr.ntgitg.mineglot.core.service.message;

import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.utils.prefix.Prefix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public final class MessageService {

    private MessageService() {
    }

    public static void sendMessage(EntityPlayer player, String messageKey, Object... args) {
        sendMessage(player, messageKey, true, args);
    }

    public static void sendMessage(EntityPlayer player, String messageKey, boolean withPrefix,
                                   Object... args) {
        if (player != null) {
            String message = I18nManager.getMessage(messageKey, args);
            String finalMessage = withPrefix ? Prefix.getMainPrefix() + message : message;
            player.addChatMessage(new ChatComponentText(finalMessage));
        }
    }

    public static void sendSuccess(EntityPlayer player, String messageKey, Object... args) {
        sendSuccess(player, messageKey, true, args);
    }

    public static void sendSuccess(EntityPlayer player, String messageKey, boolean withPrefix,
                                   Object... args) {
        String message = "§a" + I18nManager.getMessage(messageKey, args);
        if (player != null) {
            String finalMessage = withPrefix ? Prefix.getMainPrefix() + message : message;
            player.addChatMessage(new ChatComponentText(finalMessage));
        }
    }

    public static void sendError(EntityPlayer player, String messageKey, Object... args) {
        sendError(player, messageKey, true, args);
    }

    public static void sendError(EntityPlayer player, String messageKey, boolean withPrefix,
                                 Object... args) {
        String message = "§c" + I18nManager.getMessage(messageKey, args);
        if (player != null) {
            String finalMessage = withPrefix ? Prefix.getMainPrefix() + message : message;
            player.addChatMessage(new ChatComponentText(finalMessage));
        }
    }

    public static void sendInfo(EntityPlayer player, String messageKey, Object... args) {
        sendInfo(player, messageKey, true, args);
    }

    public static void sendInfo(EntityPlayer player, String messageKey, boolean withPrefix,
                                Object... args) {
        String message = "§7" + I18nManager.getMessage(messageKey, args);
        if (player != null) {
            String finalMessage = withPrefix ? Prefix.getMainPrefix() + message : message;
            player.addChatMessage(new ChatComponentText(finalMessage));
        }
    }
}
