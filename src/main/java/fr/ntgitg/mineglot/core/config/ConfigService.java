package fr.ntgitg.mineglot.core.config;

import fr.ntgitg.mineglot.core.service.AbstractService;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigService extends AbstractService {
    private static final String MOD_ID = "mineglot";

    private volatile Configuration mainConfig;
    private volatile ModConfig modConfig;
    private volatile File configFileOverride;
    private final Map<String, Configuration> configurations = new ConcurrentHashMap<>();

    ConfigService() {
        super("Configuration");
    }

    public static ConfigService getInstance() {
        return SingletonManager.getInstance(ConfigService.class, ConfigService::new);
    }

    public void setConfigFile(File configFile) {
        if (configFile == null) {
            return;
        }
        configFileOverride = configFile;
        ModLogger.info("ConfigService using config file: {}", configFile.getAbsolutePath());
    }

    public boolean preparePreInitialization(File suggestedConfigFile) {
        if (suggestedConfigFile == null) {
            ModLogger.warn("Aucun fichier de configuration suggere par Forge");
            return true;
        }

        setConfigFile(suggestedConfigFile);
        File parent = suggestedConfigFile.getParentFile();
        if (parent == null) {
            ModLogger.warn("Repertoire parent du fichier de configuration introuvable");
            return false;
        }
        if (parent.exists()) {
            return true;
        }
        if (parent.mkdirs()) {
            ModLogger.info("Repertoire de configuration cree: {}", parent.getAbsolutePath());
            return true;
        }

        ModLogger.error("Impossible de creer le repertoire de configuration: {}",
                parent.getAbsolutePath());
        return false;
    }

    public boolean saveIfChanged() {
        if (mainConfig == null) {
            ModLogger.warn("Impossible de sauvegarder la configuration: mainConfig non charge");
            return false;
        }
        if (!mainConfig.hasChanged()) {
            ModLogger.debug("Aucun changement de configuration a sauvegarder");
            return true;
        }

        mainConfig.save();
        ModLogger.info("Configuration sauvegardee");
        return true;
    }

    @Override
    protected synchronized void doStart() throws Exception {
        File configFile = resolveConfigFile();
        ensureParentDirectory(configFile);
        ensureConfigFileExists(configFile);

        try {
            mainConfig = new Configuration(configFile);
            mainConfig.load();
            modConfig = new ModConfig(mainConfig);

            if (configFile.length() == 0) {
                initializeDefaultConfiguration();
            }

            initializeConfigurations();
            validateConfigurations();

            ModLogger.info("Configuration principale chargee: {}", configFile.getAbsolutePath());
            ModLogger.info("ConfigService demarre avec succes");
        } catch (Exception e) {
            ModLogger.error("Erreur lors du demarrage du ConfigService", e);
            throw new IllegalStateException("Impossible de demarrer le ConfigService", e);
        }
    }

    @Override
    protected synchronized void doStop() throws Exception {
        saveAllConfigurations();
        cleanupConfigurations();
    }

    public String getDefaultDbPath() {
        return ConfigPathResolver.getDefaultDbPath();
    }

    public File getMinecraftConfigDir() {
        return ConfigPathResolver.getConfigDirPath().toFile();
    }

    public ModConfig getModConfig() {
        return modConfig;
    }

    public Configuration getMainConfig() {
        return mainConfig;
    }

    public Configuration getConfig(String name) {
        return configurations.get(name);
    }

    private void ensureParentDirectory(File configFile) {
        File configDir = configFile.getParentFile();
        if (configDir == null || configDir.exists()) {
            return;
        }
        if (!configDir.mkdirs()) {
            ModLogger.warn("Impossible de creer le repertoire de configuration: {}",
                    configDir.getAbsolutePath());
            return;
        }
        ModLogger.info("Repertoire de configuration cree: {}", configDir.getAbsolutePath());
    }

    private void ensureConfigFileExists(File configFile) {
        if (configFile.exists()) {
            return;
        }
        try {
            if (!configFile.createNewFile()) {
                ModLogger.warn("Le fichier de configuration n'a pas pu etre cree: {}",
                        configFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer le fichier de configuration", e);
        }
    }

    private void initializeDefaultConfiguration() {
        if (mainConfig == null) {
            return;
        }
        mainConfig.save();
        ModLogger.debug("Structure de configuration par defaut initialisee");
    }

    private void initializeConfigurations() {
        configurations.clear();
        configurations.put("main", mainConfig);
        configurations.put("translation", mainConfig);
        configurations.put("language", mainConfig);
        configurations.put("api", mainConfig);
        configurations.put("hud", mainConfig);
        configurations.put("database", mainConfig);
        configurations.put("log", mainConfig);
        configurations.put("gui", mainConfig);
        configurations.put("github", mainConfig);
        configurations.put("cache", mainConfig);
    }

    private void validateConfigurations() {
        if (modConfig == null) {
            throw new IllegalStateException("ModConfig non initialise");
        }

        if (!modConfig.isApiKeySet()) {
            ModLogger.warn("Cle API non configuree");
        }

        String targetLang = modConfig.getTargetLanguage();
        if (targetLang == null || targetLang.trim().isEmpty()) {
            throw new IllegalStateException("Langue cible non configuree");
        }

        if (modConfig.getDbPath() == null || modConfig.getDbPath().trim().isEmpty()) {
            throw new IllegalStateException("Chemin de base de donnees non configure");
        }
    }

    private void saveAllConfigurations() {
        if (mainConfig != null && mainConfig.hasChanged()) {
            mainConfig.save();
        }
        ModLogger.info("Toutes les configurations ont ete sauvegardees");
    }

    private void cleanupConfigurations() {
        if (modConfig != null) {
            modConfig.cleanup();
        }
        configurations.clear();
        ModLogger.info("Nettoyage des configurations termine");
    }

    private File resolveConfigFile() {
        if (configFileOverride != null) {
            return configFileOverride;
        }
        return new File(getMinecraftConfigDir(), MOD_ID + ".cfg");
    }
}
