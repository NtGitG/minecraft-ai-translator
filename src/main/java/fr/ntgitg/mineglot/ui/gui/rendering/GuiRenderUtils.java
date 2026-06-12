package fr.ntgitg.mineglot.ui.gui.rendering;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class GuiRenderUtils {

    private GuiRenderUtils() {
    }

    public static class ColorRGBA {
        public final float r, g, b, a;

        public ColorRGBA(int color) {
            this.r = (color >> 16 & 255) / 255.0F;
            this.g = (color >> 8 & 255) / 255.0F;
            this.b = (color & 255) / 255.0F;
            this.a = (color >> 24 & 255) / 255.0F;
        }

        public void apply() {
            GlStateManager.color(r, g, b, a);
        }
    }

    private static void withGLState(Runnable action) {
        GlStateManager.pushAttrib();
        try {
            action.run();
        } finally {
            GlStateManager.popAttrib();
        }
    }

    private static void setupRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
    }

    private static void restoreRenderState() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        withGLState(() -> {
            setupRenderState();
            drawRectRaw(left, top, right, bottom, color);
            restoreRenderState();
        });
    }

    public static void drawRectWithBorder(int left, int top, int right, int bottom, int color,
                                          int borderColor) {
        drawRectWithBorder(left, top, right, bottom, color, borderColor, 1);
    }

    public static void drawRectWithBorder(int left, int top, int right, int bottom, int color,
                                          int borderColor, int borderWidth) {
        withGLState(() -> {
            setupRenderState();
            drawRectRaw(left, top, right, bottom, color);
            drawRectRaw(left, top, right, top + borderWidth, borderColor);
            drawRectRaw(left, bottom - borderWidth, right, bottom, borderColor);
            drawRectRaw(left, top, left + borderWidth, bottom, borderColor);
            drawRectRaw(right - borderWidth, top, right, bottom, borderColor);
            restoreRenderState();
        });
    }

    private static void drawRectRaw(int left, int top, int right, int bottom, int color) {
        new ColorRGBA(color).apply();

        WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        Tessellator.getInstance().draw();
    }

    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor,
                                        int endColor) {
        withGLState(() -> {
            setupRenderState();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            ColorRGBA start = new ColorRGBA(startColor);
            ColorRGBA end = new ColorRGBA(endColor);

            WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos(right, top, 0.0D).color(end.r, end.g, end.b, end.a).endVertex();
            worldrenderer.pos(left, top, 0.0D).color(start.r, start.g, start.b, start.a).endVertex();
            worldrenderer.pos(left, bottom, 0.0D).color(start.r, start.g, start.b, start.a).endVertex();
            worldrenderer.pos(right, bottom, 0.0D).color(end.r, end.g, end.b, end.a).endVertex();
            Tessellator.getInstance().draw();

            GlStateManager.shadeModel(GL11.GL_FLAT);
            restoreRenderState();
        });
    }

    private static int calculateCircleSegments(int radius) {
        return Math.max(12, radius / 2);
    }

    public static void drawFilledCircle(int x, int y, int radius, int color) {
        withGLState(() -> {
            setupRenderState();
            new ColorRGBA(color).apply();

            int segments = calculateCircleSegments(radius);
            WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            worldrenderer.pos(x, y, 0).endVertex();

            for (int i = 0; i <= segments; i++) {
                double angle = Math.PI * 2 * i / segments;
                double x2 = x + Math.cos(angle) * radius;
                double y2 = y + Math.sin(angle) * radius;
                worldrenderer.pos(x2, y2, 0).endVertex();
            }

            Tessellator.getInstance().draw();
            restoreRenderState();
        });
    }

    public static void drawCircle(int x, int y, int radius, int color, float lineWidth) {
        withGLState(() -> {
            setupRenderState();
            new ColorRGBA(color).apply();
            GL11.glLineWidth(lineWidth);

            int segments = calculateCircleSegments(radius);
            WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
            worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

            for (int i = 0; i < segments; i++) {
                double angle = Math.PI * 2 * i / segments;
                double x2 = x + Math.cos(angle) * radius;
                double y2 = y + Math.sin(angle) * radius;
                worldrenderer.pos(x2, y2, 0).endVertex();
            }

            Tessellator.getInstance().draw();
            restoreRenderState();
        });
    }

    public static void drawCircle(int x, int y, int radius, int color) {
        drawCircle(x, y, radius, color, 1.0f);
    }
}
