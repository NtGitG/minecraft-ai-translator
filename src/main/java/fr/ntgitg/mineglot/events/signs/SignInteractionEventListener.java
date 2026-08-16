package fr.ntgitg.mineglot.events.signs;

import fr.ntgitg.mineglot.features.signs.SignTranslationHandler;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SignInteractionEventListener {

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            if (event == null || event.isCanceled()) {
                return;
            }
            if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                    || event.pos == null || event.world == null) {
                return;
            }

            TileEntity tileEntity = event.world.getTileEntity(event.pos);
            if (!(tileEntity instanceof TileEntitySign)) {
                return;
            }

            SignTranslationHandler signHandler = SignTranslationHandler.getInstance();
            if (signHandler != null) {
                signHandler.handleSignInteraction(event.pos, (TileEntitySign) tileEntity);
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la traduction manuelle de panneau", e);
        }
    }
}
