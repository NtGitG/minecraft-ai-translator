package fr.ntgitg.mineglot.ui.hud.logic;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.ui.hud.core.HUDConstants;
import fr.ntgitg.mineglot.ui.hud.rendering.HUDRenderer;
import fr.ntgitg.mineglot.utils.log.ModLogger;

public class HUDCoordinator {

    private final ConfigurationManager configManager;
    private final HUDRenderer renderer;

    public HUDCoordinator() {
        this.configManager = ConfigurationManager.getInstance();
        this.renderer = new HUDRenderer();
    }

    public void render() {
        if (!HUDConstants.HUD_ENABLED) {
            return;
        }

        try {
            renderer.render();

        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu HUD coordonné", e);
        }
    }

    public void forceReload() {
        renderer.invalidateCaches();
        ModLogger.debug("Cache HUD invalidé via renderer");
    }

    public void invalidateCache() {
        forceReload();
    }

    public void cleanup() {
        ModLogger.debug("HUDCoordinator nettoye");
    }

    public void debugStatus() {
        ModLogger.debug("=== HUD COORDINATOR STATUS ===");
        ModLogger.debug("HUD activé: {}", HUDConstants.HUD_ENABLED);
        ModLogger.debug("ConfigManager: {}", configManager != null);
        ModLogger.debug("Renderer: {}", renderer != null);
        ModLogger.debug("=============================");
    }

    public void debugCacheStatus() {
        debugStatus();
    }
}
