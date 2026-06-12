package fr.ntgitg.mineglot.core.command.translate.translate_text.handler;

import fr.ntgitg.mineglot.core.translation.TranslationOrchestrator;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.UUID;

public class TranslateCommandHandler {

    private static ConfigurationManager getConfigManager() {
        return ConfigurationManager.getInstance();
    }

    public static void handleTranslateCommand(ICommandSender sender, String[] args)
            throws CommandException {

        if (args == null || args.length < 1) {
            sendMissingTextMessage(sender);
            return;
        }

        String rawText = buildMessageFromArgs(args);
        if (rawText.isEmpty()) {
            sendMissingTextMessage(sender);
            return;
        }

        try {
            SupportedLanguage targetLang = getTargetLanguage();
            TranslationCommandRequest request = resolveTranslationRequest(args);

            ModLogger.debug("Commande /translate executee avec le texte: {}",
                    request.getText());
            ModLogger.debug("Langue cible: {}", targetLang.getDisplayName());

            executeTranslation(sender, request);

        } catch (Exception e) {
            ErrorManager.handleError(e, ErrorType.TRANSLATION, sender);
        }
    }

    private static void sendMissingTextMessage(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendError((EntityPlayer) sender, "translation.command.missing_text");
            return;
        }

        if (sender != null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED
                    + I18nManager.getMessage("translation.command.missing_text")));
        }
    }

    public static String buildMessageFromArgs(String[] args) {
        return buildMessageFromArgs(args, 0);
    }

    static String buildMessageFromArgs(String[] args, int startIndex) {
        StringBuilder messageBuilder = new StringBuilder();
        if (args == null || startIndex >= args.length) {
            return "";
        }

        int safeStart = Math.max(0, startIndex);
        for (int i = safeStart; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            messageBuilder.append(arg).append(" ");
        }
        return messageBuilder.toString().trim();
    }

    public static void executeTranslation(ICommandSender sender, String text) {
        executeTranslation(sender, TranslationCommandRequest.publicMessage(text));
    }

    private static void executeTranslation(ICommandSender sender, TranslationCommandRequest request) {
        try {
            String playerName = sender.getName();
            if (request.isPrivateMessage()) {
                TranslationOrchestrator.translatePrivateMessage(playerName, request.getTargetPlayer(),
                        request.getText());
                return;
            }

            TranslationOrchestrator.translate(playerName, request.getText(), false, false);
        } catch (Exception e) {
            ErrorManager.handleError(e, ErrorType.TRANSLATION, sender);
        }
    }

    private static TranslationCommandRequest resolveTranslationRequest(String[] args) {
        String publicText = buildMessageFromArgs(args);
        if (args == null || args.length < 2) {
            return TranslationCommandRequest.publicMessage(publicText);
        }

        String privateText = buildMessageFromArgs(args, 1);
        if (privateText.isEmpty()) {
            return TranslationCommandRequest.publicMessage(publicText);
        }

        String targetPlayer = resolveOnlinePlayerTarget(args[0]);
        if (targetPlayer == null) {
            return TranslationCommandRequest.publicMessage(publicText);
        }

        return TranslationCommandRequest.privateMessage(targetPlayer, privateText);
    }

    private static String resolveOnlinePlayerTarget(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return null;
        }

        String typedName = candidate.trim();
        PlayerNameManager playerNameManager = PlayerNameManager.getInstance();
        UUID playerId = playerNameManager.getPlayerUuidByName(typedName);
        if (playerId == null) {
            return null;
        }

        return typedName;
    }

    public static SupportedLanguage getTargetLanguage() {
        String targetLangCode = getConfigManager().getTargetLanguage();
        if (targetLangCode == null || targetLangCode.isEmpty()) {
            throw new IllegalStateException("Aucune langue cible selectionnee");
        }

        SupportedLanguage targetLang = SupportedLanguage.fromCode(targetLangCode);
        if (targetLang == null) {
            throw new IllegalArgumentException("Code de langue invalide: " + targetLangCode);
        }

        return targetLang;
    }

    public static String getFormattedUsage() {
        try {
            SupportedLanguage targetLang = getTargetLanguage();
            return String.format(
                    "\u00A7e/translate [joueur] <texte> \u00A77- Traduit et envoie le texte vers la langue cible %s",
                    targetLang.getDisplayName());
        } catch (Exception e) {
            return "\u00A7e/translate [joueur] <texte> \u00A77- Traduit et envoie le texte vers la langue cible";
        }
    }

    private static final class TranslationCommandRequest {
        private final String targetPlayer;
        private final String text;

        private TranslationCommandRequest(String targetPlayer, String text) {
            this.targetPlayer = targetPlayer;
            this.text = text != null ? text : "";
        }

        private static TranslationCommandRequest publicMessage(String text) {
            return new TranslationCommandRequest(null, text);
        }

        private static TranslationCommandRequest privateMessage(String targetPlayer, String text) {
            return new TranslationCommandRequest(targetPlayer, text);
        }

        private boolean isPrivateMessage() {
            return targetPlayer != null && !targetPlayer.isEmpty();
        }

        private String getTargetPlayer() {
            return targetPlayer;
        }

        private String getText() {
            return text;
        }
    }
}
