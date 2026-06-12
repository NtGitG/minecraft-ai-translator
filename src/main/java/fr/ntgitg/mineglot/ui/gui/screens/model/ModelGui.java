package fr.ntgitg.mineglot.ui.gui.screens.model;

import fr.ntgitg.mineglot.core.model.AIModel;
import fr.ntgitg.mineglot.core.model.ModelRegistry;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.ui.gui.base.AbstractScrollableListGui;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.ui.gui.utils.tooltip.ModelTooltipBuilder;
import fr.ntgitg.mineglot.ui.hud.manager.HUDManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ModelGui extends AbstractScrollableListGui<String> {

    public ModelGui(GuiScreen parentScreen) {
        super(parentScreen, getAllAvailableModels(), 4);
    }

    private static List<String> getAllAvailableModels() {
        Set<String> modelIds = new LinkedHashSet<>();

        for (String engine : ModelRegistry.getAvailableEngines()) {
            AIModel[] models = AIModel.getModelsForEngine(engine);
            for (AIModel model : models) {
                modelIds.add(model.getModelId());
            }
        }

        return new ArrayList<>(modelIds);
    }

    @Override
    protected String getDisplayName(String model) {
        return model;
    }

    @Override
    protected void onSelect(String modelId) {
        playButtonSound();

        try {
            AIModel selectedModel = AIModel.fromModelId(modelId);
            if (selectedModel == null) {
                ModLogger.warn("Modele introuvable: {}", modelId);
                MessageService.sendError(mc.thePlayer, "model.not_found");
                return;
            }

            String engine = selectedModel.getEngine();
            ModLogger.info("Selection du modele {} pour le moteur {}", modelId, engine);

            getConfigurationManager().setModelForEngine(engine, modelId);
            getConfigurationManager().setCurrentEngine(engine);

            MessageService.sendSuccess(mc.thePlayer, "model.selected", modelId);
            if (listComponent != null) {
                listComponent.setSelectedValue(modelId);
            }

            reloadHudSafely();

        } catch (Exception e) {
            handleConfigError(e, "selection du modele " + modelId);
        }
    }

    @Override
    protected List<String> getTooltip(String model) {
        return ModelTooltipBuilder.buildTooltip(model);
    }

    private void reloadHudSafely() {
        try {
            HUDManager hudManager = HUDManager.getInstance();
            if (hudManager != null) {
                hudManager.forceReload();
            }
        } catch (Exception hudError) {
            ModLogger.warn("Impossible de mettre a jour le HUD: {}", hudError.getMessage());
        }
    }

    @Override
    protected String getSelectedItem() {
        if (listComponent == null) {
            return null;
        }

        String currentEngine = getConfigurationManager().getCurrentEngine();
        String currentModel = getConfigurationManager().getModelForEngine(currentEngine);

        for (String model : listComponent.getDisplayItems()) {
            if (model != null && model.equals(currentModel)) {
                return model;
            }
        }

        return null;
    }

    @Override
    protected String getTitle() {
        return TitleManager.getModelTitle();
    }
}
