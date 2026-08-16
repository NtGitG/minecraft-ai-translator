package fr.ntgitg.mineglot.ui.gui.screens.update;

import net.minecraft.client.gui.GuiScreen;

@FunctionalInterface
interface UpdateScreenNavigator {
    void show(GuiScreen screen);
}
