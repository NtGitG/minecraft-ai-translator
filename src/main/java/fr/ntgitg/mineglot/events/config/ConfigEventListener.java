package fr.ntgitg.mineglot.events.config;

import fr.ntgitg.mineglot.core.cache.CacheServiceFacade;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.ServiceManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ConfigEventListener {

    private static final String MOD_ID = "mineglot";

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!MOD_ID.equals(event.modID)) {
            return;
        }

        ModLogger.info("[RELOAD] Rechargement de la configuration...");

        try {
            ConfigurationManager configManager = ConfigurationManager.getInstance();
            configManager.getConfig().load();
            configManager.invalidateCache();

            reloadAffectedServices(event);
            ModLogger.info("Configuration rechargee avec succes");
        } catch (IllegalStateException e) {
            ModLogger.warn("Configuration non operationnelle - rechargement ignore");
        } catch (Exception e) {
            ModLogger.error("Erreur lors du rechargement de la configuration", e);
        }
    }

    private void reloadAffectedServices(ConfigChangedEvent.OnConfigChangedEvent event) {
        ServiceManager serviceManager = ServiceManager.getInstance();

        boolean needsCacheRestart = hasCacheConfigChanged(event);
        boolean needsConfigRestart = hasGeneralConfigChanged(event);

        if (needsCacheRestart) {
            try {
                ModLogger.info("[RELOAD] Rechargement du service Cache");
                serviceManager.restartService(CacheServiceFacade.class);
            } catch (Exception e) {
                ModLogger.error("Echec du rechargement du service Cache", e);
            }
        }

        if (needsConfigRestart) {
            restartCritical(serviceManager, fr.ntgitg.mineglot.core.config.ConfigService.class,
                    "[RELOAD] Rechargement du service Configuration");
        }

        if (!needsCacheRestart && !needsConfigRestart) {
            ModLogger.info("[RELOAD] Rechargement des services critiques par defaut");
            try {
                serviceManager.restartServices(
                        fr.ntgitg.mineglot.core.config.ConfigService.class
                );
            } catch (Exception e) {
                ModLogger.error("Echec du rechargement des services critiques", e);
                throw new RuntimeException("Critical services restart failed", e);
            }
        }
    }

    private void restartCritical(ServiceManager serviceManager,
                                 Class<? extends fr.ntgitg.mineglot.core.service.Service> serviceClass,
                                 String logMessage) {
        try {
            ModLogger.info(logMessage);
            serviceManager.restartService(serviceClass);
        } catch (Exception e) {
            ModLogger.error("Echec du rechargement du service critique: {}",
                    serviceClass.getSimpleName(), e);
            throw new RuntimeException(serviceClass.getSimpleName() + " service restart failed", e);
        }
    }

    private boolean hasCacheConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        String category = event.configID.toLowerCase();
        return category.contains("cache")
                || category.contains("memory")
                || category.contains("size");
    }

    private boolean hasGeneralConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        String category = event.configID.toLowerCase();
        return category.contains("general")
                || category.contains("config")
                || category.contains("language")
                || category.contains("target")
                || category.contains("default");
    }
}
