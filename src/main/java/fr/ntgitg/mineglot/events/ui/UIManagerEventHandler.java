package fr.ntgitg.mineglot.events.ui;

import fr.ntgitg.mineglot.ui.core.UIManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class UIManagerEventHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        UIManager uiManager = UIManager.getInstance();
        if (!uiManager.isReady()) {
            uiManager.initialize();
            ModLogger.debug("UIManager initialise automatiquement via evenement");
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        UIManager.getInstance().cleanup();
        ModLogger.info("UIManager nettoye via deconnexion client");
    }
}
