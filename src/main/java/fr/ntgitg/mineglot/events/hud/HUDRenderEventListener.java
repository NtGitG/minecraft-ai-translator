package fr.ntgitg.mineglot.events.hud;

import fr.ntgitg.mineglot.ui.hud.manager.HUDManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HUDRenderEventListener {

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        HUDManager.getInstance().handleRenderEvent();
    }
}
