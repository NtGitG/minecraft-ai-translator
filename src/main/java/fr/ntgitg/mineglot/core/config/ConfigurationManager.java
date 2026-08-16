package fr.ntgitg.mineglot.core.config;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.atomic.AtomicLong;

public final class ConfigurationManager {

    private volatile ModConfig cachedConfig;
    private volatile boolean cacheValid = false;
    private final AtomicLong configGeneration = new AtomicLong(0L);

    private ConfigurationManager() {
    }

    public static ConfigurationManager getInstance() {
        try {
            return SingletonManager.getInstance(ConfigurationManager.class, ConfigurationManager::new);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'initialisation du gestionnaire de configuration", e);
            throw new RuntimeException("Impossible d'initialiser le gestionnaire de configuration", e);
        }
    }

    public ModConfig getConfig() {
        if (cacheValid && cachedConfig != null) {
            return cachedConfig;
        }

        synchronized (this) {
            if (cacheValid && cachedConfig != null) {
                return cachedConfig;
            }

            ConfigService configService = ConfigService.getInstance();
            if (!configService.isOperational()) {
                throw new IllegalStateException("ConfigService non operationnel");
            }

            cachedConfig = configService.getModConfig();
            cacheValid = true;
            return cachedConfig;
        }
    }

    public void invalidateCache() {
        cacheValid = false;
        cachedConfig = null;
        configGeneration.incrementAndGet();
        ModLogger.debug("Cache ConfigurationManager invalide");
    }

    /**
     * Numero de generation incremente a chaque changement de configuration.
     * Permet aux consommateurs (ex: validation) de mettre en cache un resultat
     * et de ne le recalculer que lorsque la config a reellement change.
     */
    public long getConfigGeneration() {
        return configGeneration.get();
    }

    public String getApiKey(String engine) {
        return getConfig().getApiKey(engine);
    }

    public void setApiKey(String engine, String key) {
        getConfig().setApiKey(engine, key);
        invalidateCache();
    }

    public String getCurrentEngine() {
        return getConfig().getCurrentEngine();
    }

    public void setCurrentEngine(String engine) {
        getConfig().setCurrentEngine(engine);
        invalidateCache();
    }

    public String getCurrentModel() {
        return getConfig().getSelectedModel();
    }

    public String getModelForEngine(String engine) {
        return getConfig().getCurrentModelForEngine(engine);
    }

    public void setModelForEngine(String engine, String modelId) {
        getConfig().setModelForEngine(engine, modelId);
        invalidateCache();
    }

    public String getTargetLanguage() {
        return getConfig().getTargetLanguage();
    }

    public void setTargetLanguage(String langCode) {
        getConfig().setTargetLanguage(langCode);
        invalidateCache();
    }

    public String getDefaultLanguage() {
        return getConfig().getDefaultLanguage();
    }

    public void setDefaultLanguage(String langCode) {
        getConfig().setDefaultLanguage(langCode);
        invalidateCache();
    }

    public String getUiLanguage() {
        return getConfig().getUiLanguage();
    }

    public void setUiLanguage(String langCode) {
        getConfig().setUiLanguage(langCode);
        invalidateCache();
    }

    public boolean isSignTranslationEnabled() {
        return getConfig().isSignTranslationEnabled();
    }

    public void setSignTranslationEnabled(boolean enabled) {
        getConfig().setSignTranslationEnabled(enabled);
        invalidateCache();
    }

    public int getMaxCacheMemory() {
        return getConfig().getMaxCacheMemory();
    }

    public String getDbPath() {
        return getConfig().getDbPath();
    }

    public int getThreadPoolSize() {
        return getConfig().getThreadPoolSize();
    }

    public boolean isApiKeySet() {
        return getConfig().isApiKeySet();
    }

    public String getSelectedModel() {
        return getConfig().getSelectedModel();
    }

    public void debugCacheStatus() {
        ModLogger.debug("ConfigurationManager - cache valide: {}, config null: {}", cacheValid,
                cachedConfig == null);
    }
}
