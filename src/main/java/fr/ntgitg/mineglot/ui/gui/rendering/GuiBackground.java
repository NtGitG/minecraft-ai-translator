package fr.ntgitg.mineglot.ui.gui.rendering;

import fr.ntgitg.mineglot.ui.core.BackgroundRenderer;
import net.minecraft.util.ResourceLocation;

public class GuiBackground {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            new ResourceLocation("mineglot", "textures/gui/gui_background.png");

    public static void drawModernBackground(int centerX, int centerY, int width, int height) {
        BackgroundRenderer.drawCenteredBackground(BACKGROUND_TEXTURE, centerX, centerY, width, height);
    }
}
