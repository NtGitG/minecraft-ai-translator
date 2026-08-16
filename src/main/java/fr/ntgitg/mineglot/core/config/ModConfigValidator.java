package fr.ntgitg.mineglot.core.config;

import fr.ntgitg.mineglot.core.model.AIModel;
import fr.ntgitg.mineglot.core.model.ModelRegistry;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.Locale;
import java.util.Map;

public final class ModConfigValidator {

    private ModConfigValidator() {
    }

    public static ModConfigValidator getInstance() {
        try {
            return SingletonManager.getInstance(ModConfigValidator.class, ModConfigValidator::new);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'initialisation du validateur de configuration", e);
            throw new RuntimeException("Impossible d'initialiser le validateur de configuration", e);
        }
    }

    public void validateConfig(Map<String, String> apiKeys) {
        if (apiKeys == null) {
            throw new IllegalArgumentException("Api keys map cannot be null");
        }
        ModLogger.debug("Configuration chargee - validation des cles API differee");
    }

    public void validateFullConfig(ModConfigData data) {
        if (data == null) {
            throw new IllegalArgumentException("ConfigData cannot be null");
        }

        validateConfig(data.apiKeys);
        validateEngine(data.currentEngine);
        validateLanguageCode(data.targetLanguage, "Target language");
        validateLanguageCode(data.defaultLanguage, "Default language");
        validateLanguageCode(data.uiLanguage, "UI language");
        validateModel(data.currentEngine, data.selectedModel);
        validateMaxTargetedPlayers(data.maxTargetedPlayers);
    }

    public void validateEngine(String engine) {
        if (engine == null || engine.trim().isEmpty()) {
            throw new IllegalArgumentException("Engine cannot be empty");
        }
        if (!ModelRegistry.isEngineSupported(engine)) {
            throw new IllegalStateException("Moteur d'IA non supporte : " + engine);
        }
    }

    public void validateLanguageCode(String code, String fieldName) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        if (code.length() < 2 || code.length() > 5) {
            throw new IllegalArgumentException(fieldName + " must be 2-5 characters long");
        }
    }

    public void validateModel(String engine, String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model for engine " + engine + " cannot be empty");
        }

        AIModel aiModel = AIModel.fromModelId(model);
        if (aiModel == null || !aiModel.getEngine().equals(engine.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Modele '" + model + "' non supporte pour le moteur '"
                    + engine + "'. Modeles autorises: " + getAuthorizedModelsForEngine(engine));
        }
    }

    public void validateMaxTargetedPlayers(int maxTargetedPlayers) {
        if (maxTargetedPlayers < 1 || maxTargetedPlayers > 100) {
            throw new IllegalArgumentException("Max targeted players must be between 1 and 100");
        }
    }

    private String getAuthorizedModelsForEngine(String engine) {
        if (engine == null) {
            return "aucun modele autorise";
        }

        AIModel[] models = AIModel.getModelsForEngine(engine.toLowerCase(Locale.ROOT));
        if (models.length == 0) {
            return "aucun modele autorise";
        }

        StringBuilder authorized = new StringBuilder();
        for (AIModel model : models) {
            if (authorized.length() > 0) {
                authorized.append(", ");
            }
            authorized.append(model.getModelId());
        }
        return authorized.toString();
    }
}
