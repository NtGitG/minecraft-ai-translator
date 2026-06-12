package fr.ntgitg.mineglot.core.translation;

import fr.ntgitg.mineglot.ui.gui.utils.sound.SoundManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import fr.ntgitg.mineglot.utils.prefix.Prefix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;

public final class TranslationRenderer {

    private TranslationRenderer() {
    }

    public static boolean renderTranslation(String sender, String originalMessage,
                                            String translatedMessage, boolean isTargetedPlayer,
                                            boolean isTranslationCommand) {
        return renderTranslation(sender, originalMessage, translatedMessage, isTargetedPlayer,
                isTranslationCommand, null);
    }

    public static boolean renderTranslation(String sender, String originalMessage,
                                            String translatedMessage, boolean isTargetedPlayer,
                                            boolean isTranslationCommand, String privateTarget) {
        ModLogger.debug("=== TRAITEMENT RESULTAT ===");
        ModLogger.debug("Message original: '{}'", originalMessage);
        ModLogger.debug("Message traduit recu: '{}'", translatedMessage);

        String finalTranslation = translatedMessage != null
                ? translatedMessage.replaceAll("\\r\\n", " ").trim()
                : "";
        ModLogger.debug("Traduction finale utilisee: '{}'", finalTranslation);

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft != null ? minecraft.thePlayer : null;
        if (player == null) {
            ModLogger.warn("Tentative d'affichage de traduction alors que le joueur est nul");
            return false;
        }

        try {
            if (hasPrivateTarget(privateTarget)) {
                player.sendChatMessage(buildPrivateMessageCommand(privateTarget, finalTranslation));
            } else if (isTargetedPlayer) {
                if (isTranslationCommand) {
                    player.addChatMessage(new ChatComponentText(
                            Prefix.translatedMessage(finalTranslation)));
                } else {
                    player.addChatMessage(new ChatComponentText(
                            Prefix.translatedMessage(sender, finalTranslation)));
                }
            } else {
                player.sendChatMessage(finalTranslation);
            }

            SoundManager.playSuccess();
            return true;
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'affichage de la traduction", e);
            return false;
        }
    }

    static String buildPrivateMessageCommand(String privateTarget, String translatedMessage) {
        return "/msg " + privateTarget.trim() + " " + translatedMessage;
    }

    private static boolean hasPrivateTarget(String privateTarget) {
        return privateTarget != null && !privateTarget.trim().isEmpty();
    }
}
