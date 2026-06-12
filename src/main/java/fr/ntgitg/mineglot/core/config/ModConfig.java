package fr.ntgitg.mineglot.core.config;

import fr.ntgitg.mineglot.core.config.ModConfigSchema.ConfigCategory;
import fr.ntgitg.mineglot.core.config.ModConfigSchema.ConfigProperty;
import fr.ntgitg.mineglot.core.model.AIModel;
import fr.ntgitg.mineglot.core.model.ModelRegistry;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Central runtime configuration for MineGlot. */
public final class ModConfig {

  private final Configuration config;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private volatile ModConfigData configData;
  private volatile boolean autoSave = true;

 private static final int MAX_RETRIES = 3;
  private static final int RETRY_DELAY_MS = 1000;
  private static final int TIMEOUT_MS = 5000;
  private static final int MAX_CONNECTIONS = 10;
  private static final int THREAD_POOL_SIZE = 4;
  private static final int TRANSLATION_TIMEOUT_SEC = 30;
  private static final int CACHE_SIZE = 1000;
  private static final int MAX_CACHE_MEMORY_MB = 50;
  private static final int MAX_BACKOFF_MS = 30000;

  public ModConfig(Configuration config) {
    this.config = config;
    init();
  }

  private void init() {
    for (ConfigCategory category : ConfigCategory.values()) {
      ArrayList<String> properties = new ArrayList<>();
      for (ConfigProperty property : ConfigProperty.values()) {
        if (property.getCategory() == category) {
          properties.add(property.getName());
        }
      }
      config.setCategoryPropertyOrder(category.getName(), properties);
      config.setCategoryComment(category.getName(), category.getComment());
    }

    load();
  }

  public void load() {
    lock.writeLock().lock();
    try {
      config.load();
      configData = buildConfigData();

      flushIfNeeded();

      try {
        validateConfig();
      } catch (Exception e) {
        ModLogger.warn("Validation de configuration echouee: {}", e.getMessage());
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private ModConfigData buildConfigData() {
    Map<String, String> apiKeys = loadApiKeys();
    ensureDatabaseDirectories();

    String currentEngine = normalizeEngine(getStringValue(ConfigProperty.CURRENT_ENGINE));
    String selectedModel = normalizeSelectedModel(currentEngine,
        getStringValue(ConfigProperty.SELECTED_MODEL));
    String targetLanguage = getStringValue(ConfigProperty.TARGET_LANGUAGE);
    String defaultLanguage = getStringValue(ConfigProperty.DEFAULT_LANGUAGE);
    String uiLanguage = getStringValue(ConfigProperty.UI_LANGUAGE);
    boolean signTranslationEnabled = getBooleanValue(ConfigProperty.SIGN_TRANSLATION_ENABLED);
    boolean isFrench = getBooleanValue(ConfigProperty.IS_FRENCH);
    int maxTargetedPlayers = getIntValue(ConfigProperty.MAX_TARGETED_PLAYERS);

    return new ModConfigData(currentEngine, apiKeys, selectedModel, targetLanguage,
        defaultLanguage, uiLanguage, signTranslationEnabled, isFrench,
        maxTargetedPlayers);
  }

  private String normalizeEngine(String engine) {
    String normalized = engine != null ? engine.trim().toLowerCase(java.util.Locale.ROOT) : "";
    if (ModelRegistry.isEngineSupported(normalized)) {
      return normalized;
    }

    ModLogger.warn("Moteur '{}' non supporte, retour a OpenAI", engine);
    return "openai";
  }

  private String normalizeSelectedModel(String engine, String modelId) {
    AIModel selected = AIModel.fromModelId(modelId);
    if (selected != null && selected.getEngine().equals(engine)) {
      return modelId;
    }

    AIModel[] fallbackModels = AIModel.getModelsForEngine(engine);
    String fallback = fallbackModels.length > 0 ? fallbackModels[0].getModelId() : "gpt-4o";
    ModLogger.warn("Modele '{}' non supporte pour '{}', retour a {}", modelId, engine, fallback);
    return fallback;
  }

  private Map<String, String> loadApiKeys() {
    Map<String, String> apiKeys = new HashMap<>();

    try {
      for (String engine : ModelRegistry.getAvailableEngines()) {
        try {
          ConfigProperty property = ConfigProperty.forEngine(engine);
          apiKeys.put(engine, getStringValue(property));
        } catch (IllegalArgumentException e) {
          ModLogger.warn("Moteur non supporte ignore: {}", engine);
        }
      }
    } catch (Exception e) {
      ModLogger.error("Echec de chargement des moteurs disponibles", e);
      throw e;
    }

    return apiKeys;
  }

  private void ensureDatabaseDirectories() {
    String dbPath = ConfigPathResolver.getDefaultDbPath();
    ConfigPathResolver.ensureDbDirectories(dbPath);
  }

  private void flushIfNeeded() {
    if (config.hasChanged()) {
      config.save();
    }
  }

  public void save() {
    lock.writeLock().lock();
    try {
      flushIfNeeded();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void cleanup() {
    save();
  }

  private String getStringValue(ConfigProperty property) {
    return config.get(property.getCategory().getName(), property.getName(),
        property.getDefaultString(), property.getComment()).getString();
  }

  private int getIntValue(ConfigProperty property) {
    return config.get(property.getCategory().getName(), property.getName(),
        property.getDefaultInteger(), property.getComment()).getInt();
  }

  private boolean getBooleanValue(ConfigProperty property) {
    return config.get(property.getCategory().getName(), property.getName(),
        property.getDefaultBoolean(), property.getComment()).getBoolean();
  }

  private void setStringValueDirect(ConfigProperty property, String value) {
    config.get(property.getCategory().getName(), property.getName(), property.getDefaultString(),
        property.getComment()).set(value);
  }

  private void setIntValueDirect(ConfigProperty property, int value) {
    config.get(property.getCategory().getName(), property.getName(), property.getDefaultInteger(),
        property.getComment()).set(value);
  }

  private void setBooleanValueDirect(ConfigProperty property, boolean value) {
    config.get(property.getCategory().getName(), property.getName(), property.getDefaultBoolean(),
        property.getComment()).set(value);
  }

  public void validateConfig() {
    ModConfigData data = getData();

    try {
      ModConfigValidator.getInstance().validateFullConfig(data);
    } catch (Exception e) {
      ModLogger.error("Validation de configuration echouee", e);
      throw e;
    }
  }

  public ModConfigData getData() {
    return configData;
  }

  public String getApiKey() {
    return getApiKey(getCurrentEngine());
  }

  public String getApiKey(String engine) {
    return getData().apiKeys.getOrDefault(engine, "");
  }

  public String getCurrentEngine() {
    return getData().currentEngine;
  }

  public int getMaxRetries() {
    return MAX_RETRIES;
  }

  public int getRetryDelayMs() {
    return RETRY_DELAY_MS;
  }

  public int getRetryDelay() {
    return getRetryDelayMs();
  }

  public int getTimeout() {
    return TIMEOUT_MS;
  }

  public int getMaxConnections() {
    return MAX_CONNECTIONS;
  }

  public int getThreadPoolSize() {
    return THREAD_POOL_SIZE;
  }

  public int getTranslationTimeout() {
    return TRANSLATION_TIMEOUT_SEC;
  }

  public int getCacheSize() {
    return CACHE_SIZE;
  }

  public int getMaxCacheMemory() {
    return MAX_CACHE_MEMORY_MB;
  }

  public int getMaxBackoff() {
    return MAX_BACKOFF_MS;
  }

  public String getSelectedModel() {
    return getData().selectedModel;
  }

  public String getCurrentModelForEngine(String engine) {
    String selectedModel = getSelectedModel();
    AIModel model = AIModel.fromModelId(selectedModel);
    if (model != null && model.getEngine().equals(engine.toLowerCase())) {
      return selectedModel;
    }
    AIModel[] models = AIModel.getModelsForEngine(engine);
    return models.length > 0 ? models[0].getModelId() : selectedModel;
  }

  public String getDbPath() {
    return ConfigPathResolver.getDefaultDbPath();
  }

  public String getTargetLanguage() {
    return getData().targetLanguage;
  }

  public String getDefaultLanguage() {
    return getData().defaultLanguage;
  }

  public String getUiLanguage() {
    return getData().uiLanguage;
  }

  public boolean isSignTranslationEnabled() {
    return getData().signTranslationEnabled;
  }

  public boolean isFrench() {
    return getData().isFrench;
  }

  public int getMaxTargetedPlayers() {
    return getData().maxTargetedPlayers;
  }

  public void setApiKey(String engine, String key) {
    ModLogger.debug("ModConfig.setApiKey() - engine={}, keyLength={}", engine,
        key != null ? key.length() : -1);
    updateConfig(() -> {
      ConfigProperty property = ConfigProperty.forEngine(engine);
      setStringValueDirect(property, key);
    }, true);

    String after = getApiKey(engine);
    ModLogger.debug("ModConfig.afterSave() - engine={}, storedKeyLength={}", engine,
        after != null ? after.length() : -1);
  }

  public void setCurrentEngine(String engine) {
    updateConfig(() -> {
      setStringValueDirect(ConfigProperty.CURRENT_ENGINE, engine);
    });
  }

  public void setSelectedModel(String modelId) {
    updateConfig(() -> {
      setStringValueDirect(ConfigProperty.SELECTED_MODEL, modelId);
    });
  }

  public void setCurrentModel(String modelId) {
    setSelectedModel(modelId);
  }

  public void setModelForEngine(String engine, String modelId) {
    AIModel model = AIModel.fromModelId(modelId);
    if (model != null && model.getEngine().equals(engine.toLowerCase())) {
      setSelectedModel(modelId);
    } else {
      ModLogger.warn("Modele '{}' ne correspond pas au moteur '{}'", modelId, engine);
    }
  }

  public void setTargetLanguage(String code) {
    updateConfig(() -> {
      setStringValueDirect(ConfigProperty.TARGET_LANGUAGE, code);
    });
  }

  public void setDefaultLanguage(String code) {
    updateConfig(() -> {
      setStringValueDirect(ConfigProperty.DEFAULT_LANGUAGE, code);
    });
  }

  public void setUiLanguage(String code) {
    updateConfig(() -> {
      setStringValueDirect(ConfigProperty.UI_LANGUAGE, code);
    });
  }

  public void setSignTranslationEnabled(boolean enabled) {
    updateConfig(() -> {
      setBooleanValueDirect(ConfigProperty.SIGN_TRANSLATION_ENABLED, enabled);
    });
  }

  public void setFrench(boolean enabled) {
    updateConfig(() -> {
      setBooleanValueDirect(ConfigProperty.IS_FRENCH, enabled);
    });
  }

  public void setMaxTargetedPlayers(int count) {
    updateConfig(() -> {
      setIntValueDirect(ConfigProperty.MAX_TARGETED_PLAYERS, count);
    });
  }

  private void updateConfig(Runnable updateAction) {
    updateConfig(updateAction, false);
  }

  private void updateConfig(Runnable updateAction, boolean forceSave) {
    lock.writeLock().lock();
    try {
      updateAction.run();
      reloadConfigData();
      if (forceSave) {
        config.save();
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public Configuration getConfig() {
    return config;
  }

  public boolean isApiKeySet() {
    return !getData().apiKeys.isEmpty() && !getApiKey().isEmpty();
  }

  public void setAutoSave(boolean autoSave) {
    this.autoSave = autoSave;
  }

  private void reloadConfigData() {
    configData = buildConfigData();

    if (autoSave) {
      flushIfNeeded();
    }
  }

  public void batchUpdate(Runnable updates) {
    lock.writeLock().lock();
    try {
      boolean previousAutoSave = autoSave;
      autoSave = false;
      try {
        updates.run();
      } finally {
        autoSave = previousAutoSave;
        if (autoSave) {
          flushIfNeeded();
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
