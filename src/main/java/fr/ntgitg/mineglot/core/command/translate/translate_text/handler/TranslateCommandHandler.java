package fr.ntgitg.mineglot.core.command.translate.translate_text.handler;

import fr.ntgitg.mineglot.core.translation.TranslationOrchestrator;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.UUID;

public class TranslateCommandHandler {

    private static final TranslationDispatcher ORCHESTRATOR_DISPATCHER =
            (senderName, targetPlayer, text) -> {
                if (targetPlayer != null) {
                    TranslationOrchestrator.translatePrivateMessage(senderName, targetPlayer, text);
                    return;
                }
                TranslationOrchestrator.translate(senderName, text, false, false);
            };

    private static ConfigurationManager getConfigManager() {
        return ConfigurationManager.getInstance();
    }

    public static void handleTranslateCommand(ICommandSender sender, String[] args)
            throws CommandException {
        try {
            TranslationCommandRequest request = resolveTranslationRequest(args,
                    TranslateCommandHandler::resolveOnlinePlayerTarget);
            if (request.hasError()) {
                sendRequestError(sender, request);
                return;
            }

            SupportedLanguage targetLang = getTargetLanguage();

            ModLogger.debug("Commande /translate executee avec le texte: {}",
                    request.getText());
            ModLogger.debug("Langue cible: {}", targetLang.getDisplayName());

            executeTranslation(sender, request);

        } catch (Exception e) {
            ErrorManager.handleError(e, ErrorType.TRANSLATION, sender);
        }
    }

    private static void sendRequestError(ICommandSender sender,
                                         TranslationCommandRequest request) {
        if ("translation.command.missing_text".equals(request.getErrorKey())) {
            sendMissingTextMessage(sender);
            return;
        }

        Object[] errorArgs = request.getErrorArgs();
        if (sender instanceof EntityPlayer) {
            MessageService.sendError((EntityPlayer) sender, request.getErrorKey(), errorArgs);
            return;
        }

        if (sender != null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED
                    + I18nManager.getMessage(request.getErrorKey(), errorArgs)));
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
        dispatchTranslation(sender, request, ORCHESTRATOR_DISPATCHER);
    }

    static boolean dispatchTranslation(ICommandSender sender, TranslationCommandRequest request,
                                       TranslationDispatcher dispatcher) {
        if (request == null || request.hasError() || dispatcher == null) {
            return false;
        }

        try {
            String playerName = sender.getName();
            dispatcher.dispatch(playerName, request.getTargetPlayer(), request.getText());
            return true;
        } catch (Exception e) {
            ErrorManager.handleError(e, ErrorType.TRANSLATION, sender);
            return false;
        }
    }

    static TranslationCommandRequest resolveTranslationRequest(String[] args,
                                                               TargetPlayerResolver resolver) {
        String publicText = buildMessageFromArgs(args);
        if (publicText.isEmpty()) {
            return TranslationCommandRequest.error("translation.command.missing_text");
        }

        if (args == null || args.length < 1 || !"msg".equalsIgnoreCase(trim(args[0]))) {
            return TranslationCommandRequest.publicMessage(publicText);
        }

        String requestedTarget = args.length > 1 ? trim(args[1]) : "";
        String privateText = buildMessageFromArgs(args, 2);
        if (requestedTarget.isEmpty() || privateText.isEmpty()) {
            return TranslationCommandRequest.error("translation.command.missing_text");
        }

        String targetPlayer = resolver != null ? resolver.resolve(requestedTarget) : null;
        if (targetPlayer == null) {
            return TranslationCommandRequest.error(
                    "translation.command.recipient_not_found", requestedTarget);
        }

        return TranslationCommandRequest.privateMessage(targetPlayer, privateText);
    }

    private static String resolveOnlinePlayerTarget(String candidate) {
        String typedName = trim(candidate);
        if (!ValidationService.isValidPlayerNameSimple(typedName)) {
            return null;
        }

        PlayerNameManager playerNameManager = PlayerNameManager.getInstance();
        UUID playerId = playerNameManager.getPlayerUuidByName(typedName);
        if (playerId == null) {
            return null;
        }

        return typedName;
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
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
                    "\u00A7e/trs <texte> \u00A77ou \u00A7e/trs msg <joueur> <texte> \u00A77- Traduit vers %s",
                    targetLang.getDisplayName());
        } catch (Exception e) {
            return "\u00A7e/trs <texte> \u00A77ou \u00A7e/trs msg <joueur> <texte> \u00A77- Traduit le texte";
        }
    }

    interface TargetPlayerResolver {
        String resolve(String requestedTarget);
    }

    interface TranslationDispatcher {
        void dispatch(String senderName, String targetPlayer, String text);
    }

    static final class TranslationCommandRequest {
        private final String targetPlayer;
        private final String text;
        private final String errorKey;
        private final Object[] errorArgs;

        private TranslationCommandRequest(String targetPlayer, String text, String errorKey,
                                          Object[] errorArgs) {
            this.targetPlayer = targetPlayer;
            this.text = text != null ? text : "";
            this.errorKey = errorKey;
            this.errorArgs = errorArgs != null ? errorArgs.clone() : new Object[0];
        }

        private static TranslationCommandRequest publicMessage(String text) {
            return new TranslationCommandRequest(null, text, null, null);
        }

        private static TranslationCommandRequest privateMessage(String targetPlayer, String text) {
            return new TranslationCommandRequest(targetPlayer, text, null, null);
        }

        private static TranslationCommandRequest error(String errorKey, Object... errorArgs) {
            return new TranslationCommandRequest(null, "", errorKey, errorArgs);
        }

        boolean isPrivateMessage() {
            return targetPlayer != null && !targetPlayer.isEmpty();
        }

        boolean hasError() {
            return errorKey != null && !errorKey.isEmpty();
        }

        String getTargetPlayer() {
            return targetPlayer;
        }

        String getText() {
            return text;
        }

        String getErrorKey() {
            return errorKey;
        }

        Object[] getErrorArgs() {
            return errorArgs.clone();
        }
    }
}
