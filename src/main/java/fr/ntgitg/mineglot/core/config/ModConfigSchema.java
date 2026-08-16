package fr.ntgitg.mineglot.core.config;

public final class ModConfigSchema {

  private ModConfigSchema() {
  }

  public enum ConfigType {
    STRING, INTEGER, BOOLEAN, DOUBLE
  }

  public enum ConfigCategory {
    API("api", "Configuration des API de traduction"), LANGUAGE("language",
        "Configuration des langues"), TRANSLATION("translation", "Configuration de la traduction");

    private final String name;
    private final String comment;

    ConfigCategory(String name, String comment) {
      this.name = name;
      this.comment = comment;
    }

    public String getName() {
      return name;
    }

    public String getComment() {
      return comment;
    }
  }

    public enum ConfigProperty {
    CURRENT_ENGINE("currentEngine", ConfigCategory.API, ConfigType.STRING, "openai",
        "Moteur de traduction par defaut"), OPENAI_API_KEY("openaiApiKey", ConfigCategory.API,
        ConfigType.STRING, "", "Cle API OpenAI"), CLAUDE_API_KEY("claudeApiKey",
        ConfigCategory.API, ConfigType.STRING, "",
        "Cle API Claude"), SELECTED_MODEL(
        "selectedModel", ConfigCategory.API, ConfigType.STRING, "gpt-4o",
        "Modele selectionne pour la traduction (voir la liste dans l'interface)"),
    TARGET_LANGUAGE("targetLanguage", ConfigCategory.LANGUAGE, ConfigType.STRING, "fr",
        "Code langue cible"), DEFAULT_LANGUAGE("defaultLanguage", ConfigCategory.LANGUAGE,
        ConfigType.STRING, "en", "Langue par defaut"), UI_LANGUAGE("uiLanguage",
        ConfigCategory.LANGUAGE, ConfigType.STRING, "fr", "Langue de l'interface"),
    SIGN_TRANSLATION_ENABLED("signTranslationEnabled", ConfigCategory.TRANSLATION, ConfigType.BOOLEAN, false,
        "Traduction des panneaux"), IS_FRENCH("isFrench", ConfigCategory.TRANSLATION,
        ConfigType.BOOLEAN, false, "Interface en francais"), MAX_TARGETED_PLAYERS(
        "maxTargetedPlayers", ConfigCategory.TRANSLATION, ConfigType.INTEGER, 10,
        "Nombre maximum de joueurs cibles");

    private final String name;
    private final ConfigCategory category;
    private final ConfigType type;
    private final Object defaultValue;
    private final String comment;

    ConfigProperty(String name, ConfigCategory category, ConfigType type, Object defaultValue,
        String comment) {
      this.name = name;
      this.category = category;
      this.type = type;
      this.defaultValue = defaultValue;
      this.comment = comment;
    }

    public String getName() {
      return name;
    }

    public ConfigCategory getCategory() {
      return category;
    }

    public ConfigType getType() {
      return type;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }

    public String getComment() {
      return comment;
    }

    public String getDefaultString() {
      return (String) defaultValue;
    }

    public Integer getDefaultInteger() {
      return (Integer) defaultValue;
    }

    public Boolean getDefaultBoolean() {
      return (Boolean) defaultValue;
    }

    public Double getDefaultDouble() {
      return (Double) defaultValue;
    }

    public static ConfigProperty forEngine(String engine) {
      switch (engine.toLowerCase()) {
        case "openai":
          return OPENAI_API_KEY;
        case "claude":
          return CLAUDE_API_KEY;
        default:
          throw new IllegalArgumentException("Moteur non supporte: " + engine);
      }
    }
  }
}
