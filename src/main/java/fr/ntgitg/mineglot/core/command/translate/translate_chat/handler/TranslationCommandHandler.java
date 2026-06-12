package fr.ntgitg.mineglot.core.command.translate.translate_chat.handler;

import fr.ntgitg.mineglot.core.translation.TranslationOrchestrator;
import fr.ntgitg.mineglot.core.command.translate.translate_chat.services.ChatSelectionDecorator;
import fr.ntgitg.mineglot.core.command.translate.translate_chat.services.ChatSelectionService;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public final class TranslationCommandHandler {

    private TranslationCommandHandler() {
    }

    public static void handleTranslationCommand(ICommandSender sender, String[] args)
            throws CommandException {
        try {
            ModLogger.debug("=== COMMANDE TRANSLATION ===");

            if (args.length > 0) {
                String text = buildMessageFromArgs(args);
                handleDirectTranslation(sender, text);
            } else {
                enableSelectionMode(sender);
            }
        } catch (Exception e) {
            handleTranslationError(sender, e);
            throw new CommandException(I18nManager.getMessage("translation.error.processing"));
        }
    }

    public static void handleDirectTranslation(ICommandSender sender, String text) {
        ModLogger.debug("Traduction directe: {}", text);
        ChatSelectionService.getInstance().setSelecting(false);
        TranslationOrchestrator.translate(text, true);
    }

    public static void enableSelectionMode(ICommandSender sender) {
        ModLogger.debug("Activation du mode selection");
        int decoratedMessages = ChatSelectionDecorator.decorateCurrentChatHistory();
        ModLogger.debug("Messages de l'historique rendus cliquables: {}", decoratedMessages);
        sendLocalizedMessage(sender, "translation.progress");
        ChatSelectionService.getInstance().setSelecting(true);
    }

    public static String buildMessageFromArgs(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                message.append(' ');
            }
            message.append(args[i]);
        }
        return message.toString();
    }

    public static boolean isSelectionModeActive() {
        return ChatSelectionService.getInstance().isSelecting();
    }

    public static void disableSelectionMode() {
        ChatSelectionService.getInstance().setSelecting(false);
    }

    public static void handleSelectedMessage(String selectedText) {
        try {
            ModLogger.debug("Message selectionne pour traduction: {}", selectedText);
            TranslationOrchestrator.translate(selectedText, true);
            disableSelectionMode();
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la traduction du message selectionne", e);
        }
    }

    private static void handleTranslationError(ICommandSender sender, Exception e) {
        ModLogger.error("Erreur lors de l'execution de la commande translation", e);
        sendLocalizedError(sender, "translation.error.processing");
    }

    private static void sendLocalizedMessage(ICommandSender sender, String i18nKey, Object... args) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendInfo((EntityPlayer) sender, i18nKey, args);
            return;
        }
        if (sender != null) {
            String message = I18nManager.getMessage(i18nKey, args);
            sender.addChatMessage(new ChatComponentText(message));
        }
    }

    private static void sendLocalizedError(ICommandSender sender, String i18nKey, Object... args) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendError((EntityPlayer) sender, i18nKey, args);
            return;
        }
        if (sender != null) {
            String message = I18nManager.getMessage(i18nKey, args);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
        }
    }
}
