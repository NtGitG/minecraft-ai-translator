package fr.ntgitg.mineglot.ui.hud.manager;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.ui.hud.core.HUDConstants;
import fr.ntgitg.mineglot.ui.hud.logic.HUDCoordinator;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;

public class HUDManager {
    private final HUDCoordinator optimizedHUD;
    private boolean isEnabled = true;

    private HUDManager() {
        this.optimizedHUD = new HUDCoordinator();
    }

    public static HUDManager getInstance() {
        return SingletonManager.getInstance(HUDManager.class, HUDManager::new);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void handleRenderEvent() {
        if (!isEnabled || !HUDConstants.HUD_ENABLED) {
            return;
        }

        try {
            optimizedHUD.render();
        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu HUD simplifié", e);
        }
    }

    public void forceReload() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(() -> {
                optimizedHUD.forceReload();
                ModLogger.debug("HUD optimise - cache invalide");
            });
        } else {
            optimizedHUD.forceReload();
            ModLogger.debug("HUD optimise - cache invalide");
        }
    }

    public void cleanup() {
        optimizedHUD.cleanup();
        ModLogger.info("HUDManager cleanup terminé");
    }

    public static void resetInstance() {
        HUDManager instance = SingletonManager.getExistingInstance(HUDManager.class);
        if (instance != null) {
            instance.cleanup();
            SingletonManager.removeInstance(HUDManager.class);
            ModLogger.info("HUDManager instance réinitialisée");
        }
    }

    public void debugCacheStatus() {
        optimizedHUD.debugCacheStatus();
    }
}
