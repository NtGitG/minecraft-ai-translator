package fr.ntgitg.mineglot.ui.gui.screens.update;

import fr.ntgitg.mineglot.core.update.ReleaseInfo;
import net.minecraft.client.gui.GuiScreen;

interface UpdateClientBridge {
    void runOnClientThread(Runnable task);

    GuiScreen getCurrentScreen();

    void showPrompt(GuiScreen parentScreen, ReleaseInfo releaseInfo,
                    String currentVersion, Runnable dismissAction);
}
