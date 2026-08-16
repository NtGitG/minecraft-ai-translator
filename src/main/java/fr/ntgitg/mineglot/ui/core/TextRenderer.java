package fr.ntgitg.mineglot.ui.core;

import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.FontRenderer;

public final class TextRenderer {

    private TextRenderer() {
    }

    public static class TextStyle {
        public static final int WHITE = 0xFFFFFF;

        public static final int TITLE = 0xFFD700; // Doré
        public static final int CONTENT = 0xFFFFFF; // Blanc
        public static final int PLACEHOLDER = 0xFFAAAAAA; // Gris clair
        public static final int SUCCESS = 0x55FF55; // Vert
        public static final int ERROR = 0xFF5555; // Rouge

        public static final int DISABLED = 0xA0A0A0; // Gris désactivé
    }

    public static class RenderOptions {
        private boolean shadow = true;
        private float scale = 1.0f;
        private int color = TextStyle.WHITE;
        private boolean centered = false;

        public static RenderOptions defaults() {
            return new RenderOptions();
        }

        public RenderOptions withShadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        public RenderOptions withScale(float scale) {
            this.scale = scale;
            return this;
        }

        public RenderOptions withColor(int color) {
            this.color = color;
            return this;
        }

        public RenderOptions centered() {
            this.centered = true;
            return this;
        }

        public boolean hasShadow() {
            return shadow;
        }

        public float getScale() {
            return scale;
        }

        public int getColor() {
            return color;
        }

        public boolean isCentered() {
            return centered;
        }
    }

    public static void drawText(FontRenderer fontRenderer, String text, int x, int y,
                                RenderOptions options) {
        if (text == null || fontRenderer == null) {
            if (text == null)
                ModLogger.warn("Tentative de dessiner un texte null");
            return;
        }

        if (options.getScale() != 1.0f) {
            drawScaledText(fontRenderer, text, x, y, options);
        } else {
            drawSimpleText(fontRenderer, text, x, y, options);
        }
    }

    private static void drawSimpleText(FontRenderer fontRenderer, String text, int x, int y,
                                       RenderOptions options) {
        int drawX = x;
        if (options.isCentered()) {
            drawX = x - fontRenderer.getStringWidth(text) / 2;
        }

        if (options.hasShadow()) {
            fontRenderer.drawStringWithShadow(text, drawX, y, options.getColor());
        } else {
            fontRenderer.drawString(text, drawX, y, options.getColor());
        }
    }

    private static void drawScaledText(FontRenderer fontRenderer, String text, int x, int y,
                                       RenderOptions options) {
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glScalef(options.getScale(), options.getScale(), 1.0f);

        float scaledX = x / options.getScale();
        float scaledY = y / options.getScale();

        if (options.isCentered()) {
            scaledX = scaledX - (fontRenderer.getStringWidth(text) / 2);
        }

        if (options.hasShadow()) {
            fontRenderer.drawStringWithShadow(text, (int) scaledX, (int) scaledY, options.getColor());
        } else {
            fontRenderer.drawString(text, (int) scaledX, (int) scaledY, options.getColor());
        }

        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    public static void drawCenteredText(FontRenderer fontRenderer, String text, int x, int y) {
        drawText(fontRenderer, text, x, y, RenderOptions.defaults().centered());
    }

    public static void drawCenteredText(FontRenderer fontRenderer, String text, int x, int y,
                                        int color) {
        drawText(fontRenderer, text, x, y, RenderOptions.defaults().centered().withColor(color));
    }

    public static void drawCenteredTextNoShadow(FontRenderer fontRenderer, String text, int x, int y,
                                                int color) {
        drawText(fontRenderer, text, x, y,
                RenderOptions.defaults().centered().withShadow(false).withColor(color));
    }

    public static void drawTextWithShadow(FontRenderer fontRenderer, String text, int x, int y,
                                          int color) {
        drawText(fontRenderer, text, x, y, RenderOptions.defaults().withColor(color));
    }

    public static void drawTextNoShadow(FontRenderer fontRenderer, String text, int x, int y,
                                        int color) {
        drawText(fontRenderer, text, x, y, RenderOptions.defaults().withShadow(false).withColor(color));
    }

    public static void drawTitle(FontRenderer fontRenderer, String title, int x, int y) {
        drawCenteredText(fontRenderer, title, x, y, TextStyle.TITLE);
    }

    public static void drawContent(FontRenderer fontRenderer, String content, int x, int y) {
        drawTextWithShadow(fontRenderer, content, x, y, TextStyle.CONTENT);
    }

    public static void drawPlaceholder(FontRenderer fontRenderer, String placeholder, int x, int y) {
        drawTextNoShadow(fontRenderer, placeholder, x, y, TextStyle.PLACEHOLDER);
    }

    public static void drawHUDText(FontRenderer fontRenderer, String text, int x, int y, float scale,
                                   int color) {

        net.minecraft.client.renderer.GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawText(fontRenderer, text, x, y, RenderOptions.defaults().withScale(scale).withColor(color));
    }

    public static void drawButtonText(FontRenderer fontRenderer, String text, int x, int y,
                                      boolean enabled) {
        int color = enabled ? TextStyle.WHITE : TextStyle.DISABLED;
        drawCenteredText(fontRenderer, text, x, y, color);
    }

    public static void drawTooltipText(FontRenderer fontRenderer, String text, int x, int y,
                                       boolean isTitle) {
        if (isTitle) {
            drawTextNoShadow(fontRenderer, text, x, y, TextStyle.TITLE);
        } else {
            drawTextNoShadow(fontRenderer, text, x, y, TextStyle.CONTENT);
        }
    }

    public static int getTextWidth(FontRenderer fontRenderer, String text, float scale) {
        if (text == null || fontRenderer == null)
            return 0;
        return (int) (fontRenderer.getStringWidth(text) * scale);
    }

    public static int getTextHeight(FontRenderer fontRenderer, float scale) {
        if (fontRenderer == null)
            return 0;
        return (int) (fontRenderer.FONT_HEIGHT * scale);
    }

    public static int getCenteredX(FontRenderer fontRenderer, String text, int containerX,
                                   int containerWidth) {
        if (text == null || fontRenderer == null)
            return containerX;
        int textWidth = fontRenderer.getStringWidth(text);
        return containerX + (containerWidth - textWidth) / 2;
    }

    public static int getCenteredY(int containerY, int containerHeight, FontRenderer fontRenderer) {
        if (fontRenderer == null)
            return containerY;
        return containerY + (containerHeight - fontRenderer.FONT_HEIGHT) / 2;
    }
}
