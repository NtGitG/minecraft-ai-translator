package fr.ntgitg.mineglot.ui.gui.screens.language;

import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.ui.gui.base.AbstractScrollableListGui;
import fr.ntgitg.mineglot.ui.gui.utils.tooltip.TooltipManager;
import fr.ntgitg.mineglot.ui.hud.manager.HUDManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractLanguageSelectionGui
        extends AbstractScrollableListGui<SupportedLanguage> {

    private final boolean targetLanguageMode;
    private final String successMessageKey;
    private final String errorContext;
    private final Supplier<String> titleProvider;

    protected AbstractLanguageSelectionGui(GuiScreen parentScreen, boolean targetLanguageMode,
                                           String successMessageKey, String errorContext,
                                           Supplier<String> titleProvider) {
        super(parentScreen, createLanguageList(), 4);
        this.targetLanguageMode = targetLanguageMode;
        this.successMessageKey = successMessageKey;
        this.errorContext = errorContext;
        this.titleProvider = titleProvider;
    }

    private static List<SupportedLanguage> createLanguageList() {
        List<SupportedLanguage> languages = new java.util.ArrayList<>();
        for (SupportedLanguage language : SupportedLanguage.values()) {
            if (language != SupportedLanguage.AUTO) {
                languages.add(language);
            }
        }
        return languages;
    }

    @Override
    protected String getDisplayName(SupportedLanguage lang) {
        return lang.getDisplayName();
    }

    @Override
    protected void onSelect(SupportedLanguage lang) {
        playButtonSound();

        try {
            String languageCode = lang.getCode();
            setLanguageInConfig(languageCode);

            HUDManager.getInstance().forceReload();
            MessageService.sendSuccess(mc.thePlayer, getSuccessMessageKey(), lang.getDisplayName());
            if (listComponent != null) {
                listComponent.setSelectedValue(lang);
            }

            ModLogger.info("Langue definie: {} ({})", lang.getDisplayName(), languageCode);

        } catch (Exception e) {
            handleConfigError(e, getErrorContext());
        }
    }

    @Override
    protected List<String> getTooltip(SupportedLanguage lang) {
        return TooltipManager.buildTooltipLines(lang.getEnglishName(),
                TooltipManager.COLOR_INFO + I18nManager.getMessage("language.code") + " : "
                        + lang.getCode().toUpperCase());
    }

    @Override
    protected SupportedLanguage getSelectedItem() {
        if (listComponent == null) {
            return null;
        }

        try {
            String selectedCode = getLanguageFromConfig();

            for (SupportedLanguage lang : listComponent.getDisplayItems()) {
                if (lang != null && lang.getCode().equals(selectedCode)) {
                    return lang;
                }
            }

            return null;

        } catch (Exception e) {
            ModLogger.warn("Impossible de recuperer la langue selectionnee - aucune selection");
            return null;
        }
    }

    protected final void setLanguageInConfig(String languageCode) {
        if (targetLanguageMode) {
            getConfigurationManager().setTargetLanguage(languageCode);
            return;
        }
        getConfigurationManager().setDefaultLanguage(languageCode);
    }

    protected final String getLanguageFromConfig() {
        if (targetLanguageMode) {
            return getConfigurationManager().getTargetLanguage();
        }
        return getConfigurationManager().getDefaultLanguage();
    }

    protected final String getSuccessMessageKey() {
        return successMessageKey;
    }

    protected final String getErrorContext() {
        return errorContext;
    }

    @Override
    protected final String getTitle() {
        return titleProvider.get();
    }
}
