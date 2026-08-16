package fr.ntgitg.mineglot.core.service.error;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.utils.prefix.Prefix;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;

public class ErrorManager {

    public static void handleError(ModException e, ErrorType type, ICommandSender sender) {
        if (e == null || type == null) {
            ModLogger.error("Tentative de gestion d'erreur avec paramètres null");
            return;
        }

        ModLogger.error("[" + type.getDisplayName() + "] " + e.getMessage(), e);

        MinecraftForge.EVENT_BUS.post(new ModErrorEvent(e, type.name()));

        if (sender != null) {
            String userMessage = getErrorMessage(type, e.getMessage());
            if (sender instanceof EntityPlayer) {
                sender.addChatMessage(
                        new ChatComponentText(Prefix.getMainPrefix() + "§c" + userMessage));
            } else {
                sender.addChatMessage(new ChatComponentText(userMessage));
            }
        }
    }

    public static void handleError(Exception e, ErrorType type, ICommandSender sender) {
        ModException modException = new ModException(e.getMessage(), e);
        handleError(modException, type, sender);
    }

    private static String getErrorMessage(ErrorType type, String message) {
        switch (type) {
            case PLAYER:
                return I18nManager.getMessage("error.unknown");
            case CONFIG:
                return I18nManager.getMessage("error.config", message);
            case TRANSLATION:
                return I18nManager.getMessage("error.translation");
            case API:
                return getSpecificApiErrorMessage(message);
            case UI:
                return I18nManager.getMessage("error.ui", message);
            case RENDERING:
                return I18nManager.getMessage("error.rendering", message);
            case SYSTEM:
                return I18nManager.getMessage("error.system", message);
            case DATABASE:
                return I18nManager.getMessage("error.database", message);
            default:
                return I18nManager.getMessage("error.system", message);
        }
    }

    private static String getSpecificApiErrorMessage(String message) {
        if (message == null) {
            return I18nManager.getMessage("error.api", "Erreur inconnue");
        }

        if (message.contains("Connection") || message.contains("connect")
                || message.contains("network")) {
            return I18nManager.getMessage("error.api.network_error");
        }

        if (message.contains("timeout") || message.contains("Timeout")) {
            return I18nManager.getMessage("error.api.timeout");
        }

        if (message.contains("JSON") || message.contains("parse") || message.contains("parsing")) {
            return I18nManager.getMessage("error.api.parsing_error");
        }

        if (message.contains("quota") || message.contains("Quota") || message.contains("limit")) {
            return I18nManager.getMessage("error.api.quota_exceeded");
        }

        if (message.contains("invalid") && message.contains("key") || message.contains("authentication")
                || message.contains("unauthorized")) {
            return I18nManager.getMessage("error.api.invalid_key");
        }

        if (message.contains("service") && message.contains("unavailable")
                || message.contains("maintenance")) {
            return I18nManager.getMessage("error.api.service_unavailable");
        }

        if (message.contains("HTTP")) {
            if (message.contains("429")) {
                return I18nManager.getMessage("error.api.rate_limit");
            } else if (message.contains("5")) {
                return I18nManager.getMessage("error.api.server_error");
            } else if (message.contains("4")) {
                return I18nManager.getMessage("error.api.client_error");
            }
        }

        return I18nManager.getMessage("error.api", message);
    }
}
