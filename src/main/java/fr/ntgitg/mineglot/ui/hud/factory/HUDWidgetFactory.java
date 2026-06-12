package fr.ntgitg.mineglot.ui.hud.factory;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.AIModel;
import fr.ntgitg.mineglot.ui.hud.core.HUDConfig;
import fr.ntgitg.mineglot.ui.hud.core.HUDLabels;
import fr.ntgitg.mineglot.ui.hud.widgets.base.BaseHUDWidget;
import fr.ntgitg.mineglot.ui.hud.widgets.components.SimpleHUDWidget;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.ArrayList;
import java.util.List;

public class HUDWidgetFactory {

    public List<BaseHUDWidget> createWidgets() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        return createWidgets(configManager, HUDConfig.Widgets.DEFAULT_SHOW_SOURCE,
                HUDConfig.Widgets.DEFAULT_SHOW_TARGET, HUDConfig.Widgets.DEFAULT_SHOW_MODEL);
    }

    public List<BaseHUDWidget> createWidgets(ConfigurationManager configManager, boolean showSource,
                                             boolean showTarget, boolean showModel) {

        List<BaseHUDWidget> widgets = new ArrayList<>();

        try {
            if (showSource) {
                String sourceLang = configManager.getDefaultLanguage();
                widgets.add(new SimpleHUDWidget(HUDLabels.SOURCE_LABEL, formatSourceLanguage(sourceLang)));
            }
            if (showTarget) {
                String targetLang = configManager.getTargetLanguage();
                widgets.add(new SimpleHUDWidget(HUDLabels.TARGET_LABEL, formatTargetLanguage(targetLang)));
            }
            if (showModel) {
                String model = configManager.getSelectedModel();
                widgets.add(new SimpleHUDWidget(HUDLabels.MODEL_LABEL, formatModel(model), 68));
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la création des widgets HUD", e);
        }

        return widgets;
    }

    private String formatSourceLanguage(String sourceLang) {
        if (sourceLang == null || sourceLang.isEmpty()) {
            return "en";
        }
        return sourceLang;
    }

    private String formatTargetLanguage(String targetLang) {
        if (targetLang == null || targetLang.isEmpty()) {
            return "fr";
        }
        return targetLang;
    }

    private String formatModel(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return "GPT-3.5t";
        }
        AIModel model = AIModel.fromModelId(modelId);
        if (model != null) {
            return model.getDisplayName();
        }
        return modelId;
    }

    public void invalidateCache() {
        ModLogger.debug("Cache des widgets HUD invalidé");
    }
}
