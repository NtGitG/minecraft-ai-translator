package fr.ntgitg.mineglot.ui.core;

import fr.ntgitg.mineglot.ui.gui.rendering.GuiRenderUtils;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class BackgroundRenderer {

    private BackgroundRenderer() {
    }

    public static void drawBackground(ResourceLocation textureLocation, int x, int y, int width,
                                      int height, boolean useScaling) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            mc.getTextureManager().bindTexture(textureLocation);
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);

            if (useScaling) {
                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, width * 2, height * 2, width, height,
                        width * 2, height * 2);
            } else {
                Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
            }

            GlStateManager.disableBlend();
        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu du fond: " + textureLocation, e);
            drawFallbackBackground(x, y, width, height);
        }
    }

    public static void drawSimpleBackground(ResourceLocation textureLocation, int x, int y, int width,
                                            int height) {
        drawBackground(textureLocation, x, y, width, height, false);
    }

    public static void drawCenteredBackground(ResourceLocation textureLocation, int centerX,
                                              int centerY, int width, int height) {
        int x = centerX - width / 2;
        int y = centerY - height / 2;
        drawBackground(textureLocation, x, y, width, height, true);
    }

    private static void drawFallbackBackground(int x, int y, int width, int height) {
        GuiRenderUtils.drawRect(x, y, x + width, y + height, 0x80000000);
        GuiRenderUtils.drawRect(x, y, x + width, y + 1, 0xFFFFFFFF); // Top
        GuiRenderUtils.drawRect(x, y + height - 1, x + width, y + height, 0xFFFFFFFF); // Bottom
        GuiRenderUtils.drawRect(x, y, x + 1, y + height, 0xFFFFFFFF); // Left
        GuiRenderUtils.drawRect(x + width - 1, y, x + width, y + height, 0xFFFFFFFF); // Right
    }
}
