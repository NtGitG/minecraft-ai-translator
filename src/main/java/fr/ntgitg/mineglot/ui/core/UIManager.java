package fr.ntgitg.mineglot.ui.core;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Consumer;

@SideOnly(Side.CLIENT)
public final class UIManager {

    private final UIContextState context;

    private UIManager() {
        this.context = new UIContextState();
    }

    public static UIManager getInstance() {
        return SingletonManager.getInstance(UIManager.class, UIManager::new);
    }

    public void initialize() {
        boolean wasInitialized = context.isInitialized();
        if (context.initializeIfPossible()) {
            if (!wasInitialized) {
                ModLogger.info("UIManager initialise avec succes");
            }
        } else {
            if (!wasInitialized) {
                ModLogger.warn("UIManager: Minecraft ou FontRenderer non disponible");
            }
        }
    }

    public FontRenderer getFontRenderer() {
        return context.getFontRenderer();
    }

    public boolean isReady() {
        return context.isReady();
    }

    public void drawCenteredText(String text, int x, int y, int color) {
        withFontRenderer("drawCenteredText", font -> TextRenderer.drawCenteredText(font, text, x, y, color));
    }

    public void drawTextWithShadow(String text, int x, int y, int color) {
        withFontRenderer("drawTextWithShadow", font -> TextRenderer.drawTextWithShadow(font, text, x, y, color));
    }

    public void drawTextNoShadow(String text, int x, int y, int color) {
        withFontRenderer("drawTextNoShadow", font -> TextRenderer.drawTextNoShadow(font, text, x, y, color));
    }

    public void drawTitle(String title, int x, int y) {
        withFontRenderer("drawTitle", font -> TextRenderer.drawTitle(font, title, x, y));
    }

    public void drawContent(String content, int x, int y) {
        withFontRenderer("drawContent", font -> TextRenderer.drawContent(font, content, x, y));
    }

    public void drawPlaceholder(String placeholder, int x, int y) {
        withFontRenderer("drawPlaceholder", font -> TextRenderer.drawPlaceholder(font, placeholder, x, y));
    }

    public void drawHUDText(String text, int x, int y, float scale, int color) {
        withFontRenderer("drawHUDText", font -> TextRenderer.drawHUDText(font, text, x, y, scale, color));
    }

    public void drawButtonText(String text, int x, int y, boolean enabled) {
        withFontRenderer("drawButtonText", font -> TextRenderer.drawButtonText(font, text, x, y, enabled));
    }

    public void drawCenteredBackground(String texturePath, int centerX, int centerY, int width,
                                       int height) {
        try {
            BackgroundRenderer.drawCenteredBackground(toResourceLocation(texturePath), centerX, centerY,
                    width, height);
        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu du fond: {}", texturePath, e);
        }
    }

    public void drawSimpleBackground(String texturePath, int x, int y, int width, int height) {
        try {
            BackgroundRenderer.drawSimpleBackground(toResourceLocation(texturePath), x, y, width, height);
        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu du fond: {}", texturePath, e);
        }
    }

    public int[] getScreenCenter() {
        if (!context.hasMinecraft()) {
            return new int[]{0, 0};
        }
        return LayoutCalculator.calculateScreenCenter();
    }

    public int getCenteredX(int width) {
        int[] center = getScreenCenter();
        return center[0] - width / 2;
    }

    public int getCenteredY(int height) {
        int[] center = getScreenCenter();
        return center[1] - height / 2;
    }

    public int getBackButtonY(int centerY, int guiHeight, int buttonHeight) {
        return LayoutCalculator.getBackButtonY(centerY, guiHeight, buttonHeight);
    }

    public int getListStartY(int centerY, int guiHeight) {
        return LayoutCalculator.getListStartY(centerY, guiHeight);
    }

    public int getSearchFieldY(int centerY, int guiHeight) {
        return LayoutCalculator.getSearchFieldY(centerY, guiHeight);
    }

    public int getTextWidth(String text) {
        FontRenderer font = getFontRenderer();
        if (font == null || text == null) {
            return 0;
        }
        return font.getStringWidth(text);
    }

    public int getTextHeight() {
        FontRenderer font = getFontRenderer();
        return font == null ? 0 : font.FONT_HEIGHT;
    }

    public int getCenteredTextX(String text, int containerX, int containerWidth) {
        FontRenderer font = getFontRenderer();
        if (font == null || text == null) {
            return containerX;
        }
        return TextRenderer.getCenteredX(font, text, containerX, containerWidth);
    }

    public int getCenteredTextY(int containerY, int containerHeight) {
        FontRenderer font = getFontRenderer();
        if (font == null) {
            return containerY;
        }
        return TextRenderer.getCenteredY(containerY, containerHeight, font);
    }

    public String truncateText(String text, int maxWidth) {
        FontRenderer font = getFontRenderer();
        if (font == null || text == null) {
            return text;
        }
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return font.trimStringToWidth(text, maxWidth) + "...";
    }

    public void cleanup() {
        context.cleanup();
        ModLogger.info("UIManager nettoye");
    }

    public String getDebugInfo() {
        return String.format("UIManager - Ready: %s, MC: %s, Font: %s, Initialized: %s",
                isReady(), context.hasMinecraft(), getFontRenderer() != null, context.isInitialized());
    }

    public boolean isPointInRect(int mouseX, int mouseY, int rectX, int rectY, int rectWidth,
                                 int rectHeight) {
        return mouseX >= rectX && mouseX <= rectX + rectWidth && mouseY >= rectY
                && mouseY <= rectY + rectHeight;
    }

    public ScaledResolution getScaledResolution() {
        return context.getScaledResolution();
    }

    private void withFontRenderer(String operation, Consumer<FontRenderer> renderAction) {
        FontRenderer font = getFontRenderer();
        if (font == null) {
            ModLogger.debug("UIManager: {} ignore, renderer non pret", operation);
            return;
        }
        renderAction.accept(font);
    }

    private ResourceLocation toResourceLocation(String texturePath) {
        return new ResourceLocation("mineglot", texturePath);
    }
}
