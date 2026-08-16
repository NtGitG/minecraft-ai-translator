package fr.ntgitg.mineglot.ui.gui.screens.update;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;

@FunctionalInterface
interface UpdateLinkConfirmationFactory {
    GuiScreen create(GuiYesNoCallback callback, String releasePageUrl, int confirmationId);
}
