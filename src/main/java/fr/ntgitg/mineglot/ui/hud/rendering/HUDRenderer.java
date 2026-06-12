package fr.ntgitg.mineglot.ui.hud.rendering;

import fr.ntgitg.mineglot.ui.hud.cache.HUDCacheManager;
import fr.ntgitg.mineglot.ui.hud.core.HUDConfig;
import fr.ntgitg.mineglot.ui.hud.core.HUDConstants;
import fr.ntgitg.mineglot.ui.hud.factory.HUDWidgetFactory;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class HUDRenderer {
    private final HUDWidgetFactory widgetFactory;
    private final HUDCacheManager cacheManager;

    public HUDRenderer() {
        this.widgetFactory = new HUDWidgetFactory();
        this.cacheManager = new HUDCacheManager(widgetFactory);
    }

    public void render() {
        boolean matrixPushed = false;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.fontRendererObj == null) {
                return;
            }
            FontRenderer fontRenderer = mc.fontRendererObj;

            org.lwjgl.opengl.GL11.glPushMatrix();
            matrixPushed = true;
            net.minecraft.client.renderer.GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            net.minecraft.client.renderer.GlStateManager.enableAlpha();
            net.minecraft.client.renderer.GlStateManager.enableBlend();

            int x = HUDConstants.HUD_POSITION_X;
            int y = HUDConstants.HUD_POSITION_Y;

            if (HUDConfig.Widgets.SHOW_HUD_BACKGROUND) {
                HUDBackground.drawHUDBackground(x, y, HUDConstants.HUD_WIDTH, HUDConstants.HUD_HEIGHT);
            }

            for (HUDCacheManager.CachedWidgetRender cachedRender : cacheManager.getCachedRenders()) {
                cachedRender.render(fontRenderer);
            }

        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu HUD simplifié", e);
            ErrorManager.handleError(e, ErrorType.RENDERING, null);
        } finally {
            if (matrixPushed) {
                try {
                    org.lwjgl.opengl.GL11.glPopMatrix();
                } catch (Exception popError) {
                    ModLogger.error("Erreur lors de la restauration OpenGL du HUD", popError);
                }
            }
        }
    }

    public void invalidateCaches() {
        widgetFactory.invalidateCache();
        cacheManager.invalidateAndRecreateCache();
        ModLogger.debug("Cache HUD invalidé");
    }
}
