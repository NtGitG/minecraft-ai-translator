package fr.ntgitg.mineglot.core.config;

import java.util.HashMap;
import java.util.Map;

public final class ModConfigData {
  public final String currentEngine;
  public final Map<String, String> apiKeys;
  public final String selectedModel;
  public final String targetLanguage;
  public final String defaultLanguage;
  public final String uiLanguage;
  public final boolean signTranslationEnabled;
  public final boolean isFrench;
  public final int maxTargetedPlayers;

  public ModConfigData(String currentEngine, Map<String, String> apiKeys, String selectedModel,
      String targetLanguage, String defaultLanguage, String uiLanguage,
      boolean signTranslationEnabled, boolean isFrench,
      int maxTargetedPlayers) {
    this.currentEngine = currentEngine;
    this.apiKeys = new HashMap<>(apiKeys);
    this.selectedModel = selectedModel;
    this.targetLanguage = targetLanguage;
    this.defaultLanguage = defaultLanguage;
    this.uiLanguage = uiLanguage;
    this.signTranslationEnabled = signTranslationEnabled;
    this.isFrench = isFrench;
    this.maxTargetedPlayers = maxTargetedPlayers;
  }
}
