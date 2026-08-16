package fr.ntgitg.mineglot.ui.hud.rendering;

import fr.ntgitg.mineglot.ui.core.BackgroundRenderer;
import net.minecraft.util.ResourceLocation;

public class HUDBackground {

    private static final ResourceLocation HUD_BACKGROUND =
            new ResourceLocation("mineglot", "textures/gui/hud_background.png");

    public static void drawHUDBackground(int x, int y, int width, int height) {
        BackgroundRenderer.drawSimpleBackground(HUD_BACKGROUND, x, y, width, height);
    }
}
