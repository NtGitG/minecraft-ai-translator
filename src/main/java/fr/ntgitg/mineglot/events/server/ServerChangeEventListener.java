package fr.ntgitg.mineglot.events.server;

import fr.ntgitg.mineglot.ClientShutdownManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.CompletableFuture;

public class ServerChangeEventListener {

    @SubscribeEvent
    public void onClientDisconnection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        try {
            ModLogger.info("Changement de serveur detecte");
            ModLogger.debug("Nettoyage des donnees serveur lance en arriere-plan");

            ClientShutdownManager shutdownManager = ClientShutdownManager.getInstance();
            if (shutdownManager != null) {
                CompletableFuture<Void> cleanupFuture = shutdownManager.handleDisconnection();

                cleanupFuture.whenComplete((unused, error) -> {
                    if (error != null) {
                        ModLogger.error("Erreur lors du nettoyage de changement de serveur", error);
                    } else {
                        ModLogger.info("Nettoyage des donnees serveur termine avec succes");
                    }
                });

            } else {
                ModLogger.warn("ClientShutdownManager non disponible");
            }

        } catch (Exception e) {
            ModLogger.error("Erreur lors du nettoyage de changement de serveur", e);
            ModLogger.warn("Certaines donnees peuvent ne pas avoir ete nettoyees");
        }
    }
}
