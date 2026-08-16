package fr.ntgitg.mineglot.ui.gui.utils.keyboard;

import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public final class ClipboardManager {

    private ClipboardManager() {
    }

    public static void copyToClipboard(String text) {
        try {
            if (Minecraft.getMinecraft() != null) {
                GuiScreen.setClipboardString(text);
            } else {
                ModLogger
                        .error("Minecraft n'est pas initialisé : impossible de copier dans le presse-papiers.");
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la copie dans le presse-papiers (GuiScreen).", e);
        }
    }

    public static String getFromClipboard() {
        try {
            if (Minecraft.getMinecraft() != null) {
                String text = GuiScreen.getClipboardString();
                return text;
            } else {
                ModLogger.error("Minecraft n'est pas initialisé : impossible de lire le presse-papiers.");
                return null;
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la lecture du presse-papiers (GuiScreen).", e);
            return null;
        }
    }
}
