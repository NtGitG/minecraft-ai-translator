package fr.ntgitg.mineglot.ui.gui.components.scrollbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class ScrollBarRenderer {
    private static final ResourceLocation TRACK_LOC =
            new ResourceLocation("mineglot", "textures/gui/scrollbar_track.png");
    private static final ResourceLocation KNOB_LOC =
            new ResourceLocation("mineglot", "textures/gui/scrollbar_knob.png");

    private static final int TRACK_DISPLAY_WIDTH = 6; // Track reste à 6px
    private static final int KNOB_DISPLAY_WIDTH = 4; // Knob réduit à 4px
    private static final int KNOB_OFFSET_X = 1; // Centrage du knob (6-4)/2 = 1px
    private static final int TRACK_DISPLAY_HEIGHT = 128;
    private static final int KNOB_DISPLAY_HEIGHT = 16;

    private static final int TRACK_TEXTURE_WIDTH = TRACK_DISPLAY_WIDTH * 2;
    private static final int KNOB_TEXTURE_WIDTH = KNOB_DISPLAY_WIDTH * 2;
    private static final int TRACK_TEXTURE_HEIGHT = TRACK_DISPLAY_HEIGHT * 2;
    private static final int KNOB_TEXTURE_HEIGHT = KNOB_DISPLAY_HEIGHT * 2;

    private ScrollBarRenderer() {
    }

    public static void render(int x, int y, int totalHeight, int barY, int barHeight) {
        Minecraft mc = Minecraft.getMinecraft();

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);

        GlStateManager.pushAttrib();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        mc.getTextureManager().bindTexture(TRACK_LOC);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, TRACK_DISPLAY_WIDTH, totalHeight,
                TRACK_TEXTURE_WIDTH, TRACK_TEXTURE_HEIGHT);

        mc.getTextureManager().bindTexture(KNOB_LOC);
        Gui.drawModalRectWithCustomSizedTexture(x + KNOB_OFFSET_X, barY, 0, 0, KNOB_DISPLAY_WIDTH,
                barHeight, KNOB_TEXTURE_WIDTH, KNOB_TEXTURE_HEIGHT);

        GlStateManager.popAttrib();

        if (!blendEnabled) {
            GlStateManager.disableBlend();
        }
        if (!textureEnabled) {
            GlStateManager.disableTexture2D();
        }
    }
}
