package fr.ntgitg.mineglot.ui.gui.core;

import fr.ntgitg.mineglot.ui.gui.screens.api.ApiKeyGui;
import fr.ntgitg.mineglot.ui.gui.screens.config.ConfigGui;
import fr.ntgitg.mineglot.ui.gui.screens.language.DefaultLanguageGui;
import fr.ntgitg.mineglot.ui.gui.screens.language.LanguageGui;
import fr.ntgitg.mineglot.ui.gui.screens.main.MainGui;
import fr.ntgitg.mineglot.ui.gui.screens.target.SimpleTargetPlayersGui;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public final class ScreenManager {

    private ScreenManager() {
    }

    public static void openMainMenu() {
        openScreen(new MainGui(null));
    }

    public static void openApiKeyScreen() {
        openScreen(new ApiKeyGui(getCurrentScreen()));
    }

    public static void openConfigScreen() {
        openScreen(new ConfigGui(getCurrentScreen()));
    }

    public static void openLanguageScreen() {
        openScreen(new LanguageGui(getCurrentScreen()));
    }

    public static void openTargetPlayersScreen() {
        openScreen(new SimpleTargetPlayersGui(getCurrentScreen()));
    }

    public static void openDefaultLanguageScreen() {
        openScreen(new DefaultLanguageGui(getCurrentScreen()));
    }

    private static void openScreen(GuiScreen screen) {
        try {
            Minecraft.getMinecraft().displayGuiScreen(screen);

        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'ouverture du GUI: " + screen.getClass().getSimpleName(), e);
            ErrorManager.handleError(e, ErrorType.UI, null);
        }
    }

    private static GuiScreen getCurrentScreen() {
        return Minecraft.getMinecraft().currentScreen;
    }

    public static void closeCurrentScreen() {
        try {
            Minecraft.getMinecraft().displayGuiScreen(null);

        } catch (Exception e) {
            ModLogger.error("Erreur lors de la fermeture du GUI", e);
            ErrorManager.handleError(e, ErrorType.UI, null);
        }
    }
}
