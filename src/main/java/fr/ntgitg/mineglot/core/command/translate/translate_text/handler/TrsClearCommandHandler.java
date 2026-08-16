package fr.ntgitg.mineglot.core.command.translate.translate_text.handler;

import fr.ntgitg.mineglot.core.cache.TranslationCache;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class TrsClearCommandHandler {

    public static void handleTrsClearCommand(ICommandSender sender, String[] args)
            throws CommandException {
        ModLogger.debug("=== COMMANDE TRS-CLEAR ===");

        try {
            if (!(sender instanceof EntityPlayer)) {
                sendLocalizedError(sender, "command.error.player_only");
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            if (!canUseTrsClearCommand()) {
                sendLocalizedError(player, "command.trs_clear.not_available");
                return;
            }

            clearLastTranslation(player);
        } catch (Exception e) {
            ErrorManager.handleError(e, ErrorType.SYSTEM, sender);
            sendLocalizedError(sender, "command.trs_clear.error");
        }
    }

    private static boolean canUseTrsClearCommand() {
        TranslationCache cache = TranslationCache.getInstance();
        return cache.hasRecentTranslation() && !cache.isTrsClearCommandUsed();
    }

    private static void clearLastTranslation(EntityPlayer player) {
        TranslationCache cache = TranslationCache.getInstance();

        try {
            String lastText = cache.getLastTranslatedText();
            String lastTargetLang = cache.getLastTargetLanguage();
            if (lastText == null || lastTargetLang == null) {
                sendLocalizedError(player, "command.trs_clear.no_translation");
                return;
            }

            boolean success = cache.clearLastTranslation();
            if (!success) {
                sendLocalizedError(player, "command.trs_clear.failed");
                return;
            }

            cache.setTrsClearCommandUsed(true);
            sendLocalizedSuccess(player, "command.trs_clear.success", lastText);
            ModLogger.debug("Traduction supprimee du cache pour {}: {}", player.getName(), lastText);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la suppression de la traduction", e);
            sendLocalizedError(player, "command.trs_clear.error");
        }
    }

    public static String getFormattedUsage() {
        return "/trs-clear - supprime la derniere traduction du cache\n"
                + "Disponible uniquement apres /trs";
    }

    private static void sendLocalizedSuccess(EntityPlayer player, String messageKey, Object... args) {
        try {
            MessageService.sendSuccess(player, messageKey, args);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'envoi du message de succes", e);
            player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.GREEN + I18nManager.getMessage("command.trs_clear.success", "")));
        }
    }

    private static void sendLocalizedError(EntityPlayer player, String messageKey, Object... args) {
        try {
            MessageService.sendError(player, messageKey, args);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'envoi du message d'erreur", e);
            String fallback = I18nManager.getMessage("command.trs_clear.error");
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + fallback));
        }
    }

    private static void sendLocalizedError(ICommandSender sender, String messageKey, Object... args) {
        try {
            if (sender instanceof EntityPlayer) {
                sendLocalizedError((EntityPlayer) sender, messageKey, args);
                return;
            }

            if (sender != null) {
                String message = I18nManager.getMessage(messageKey, args);
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'envoi du message d'erreur", e);
            if (sender != null) {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.RED + I18nManager.getMessage("command.trs_clear.error")));
            }
        }
    }
}
