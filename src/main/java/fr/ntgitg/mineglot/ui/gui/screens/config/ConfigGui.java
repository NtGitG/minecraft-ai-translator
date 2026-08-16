package fr.ntgitg.mineglot.ui.gui.screens.config;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.ui.gui.base.AbstractReadOnlyListGui;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.ui.gui.utils.tooltip.TooltipManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigGui extends AbstractReadOnlyListGui<ConfigGui.UserConfigOption> {

    private static final String LABEL_STATE_SEPARATOR = " : ";
    private static final String INTERFACE_CYCLE_HINT = "Cliquer pour changer : EN -> FR -> JP";

    public static class UserConfigOption {

        public enum OptionType {
            INTERFACE_LANGUAGE("gui.language", "gui.language_description") {
                @Override
                boolean isEnabled(ConfigurationManager configManager) {
                    return true;
                }

                @Override
                void toggle(ConfigurationManager configManager) {
                    String nextLang = nextLanguageCode(I18nManager.getCurrentLanguage());
                    I18nManager.setLanguage(nextLang);
                    configManager.setUiLanguage(nextLang);
                }

                @Override
                String getStateMessage(ConfigurationManager configManager) {
                    return I18nManager.getLanguageDisplayName();
                }

                @Override
                void appendTooltipHint(List<String> lines) {
                    lines.add(TooltipManager.COLOR_INFO + INTERFACE_CYCLE_HINT);
                }
            },
            SIGN_TRANSLATION("gui.sign_translation", "gui.sign_translation_description") {
                @Override
                boolean isEnabled(ConfigurationManager configManager) {
                    return configManager.isSignTranslationEnabled();
                }

                @Override
                void toggle(ConfigurationManager configManager) {
                    configManager.setSignTranslationEnabled(!isEnabled(configManager));
                }
            };

            private final String labelKey;
            private final String descriptionKey;

            OptionType(String labelKey, String descriptionKey) {
                this.labelKey = labelKey;
                this.descriptionKey = descriptionKey;
            }

            abstract boolean isEnabled(ConfigurationManager configManager);

            abstract void toggle(ConfigurationManager configManager);

            String getLabel() {
                return I18nManager.getMessage(labelKey);
            }

            String getDescription() {
                return I18nManager.getMessage(descriptionKey);
            }

            String getStateMessage(ConfigurationManager configManager) {
                return isEnabled(configManager)
                        ? I18nManager.getMessage("config.option.enabled")
                        : I18nManager.getMessage("config.option.disabled");
            }

            void appendTooltipHint(List<String> lines) {
                lines.add(TooltipManager.COLOR_INFO + I18nManager.getMessage("gui.click_to_toggle"));
            }

            private static String nextLanguageCode(String currentLang) {
                if (I18nManager.ENGLISH.equals(currentLang)) {
                    return I18nManager.FRENCH;
                }
                if (I18nManager.FRENCH.equals(currentLang)) {
                    return I18nManager.JAPANESE;
                }
                return I18nManager.ENGLISH;
            }
        }

        private final OptionType type;

        public UserConfigOption(OptionType type) {
            this.type = type;
        }

        public OptionType getType() {
            return type;
        }

        public String getLabel() {
            return type.getLabel();
        }

        public String getDescription() {
            return type.getDescription();
        }

        public boolean isEnabled() {
            ConfigurationManager configManager = ConfigurationManager.getInstance();

            try {
                return type.isEnabled(configManager);
            } catch (IllegalStateException e) {
                ModLogger.warn("Configuration non operationnelle dans UserConfigOption");
                return false;
            }
        }

        public void toggle() {
            ConfigurationManager configManager = ConfigurationManager.getInstance();

            try {
                type.toggle(configManager);
            } catch (IllegalStateException e) {
                ModLogger.warn("Configuration non operationnelle - toggle ignore pour {}", type);
            }
        }

        public String getStateMessage() {
            ConfigurationManager configManager = ConfigurationManager.getInstance();
            try {
                return type.getStateMessage(configManager);
            } catch (IllegalStateException e) {
                ModLogger.warn("Configuration non operationnelle - etat indisponible pour {}", type);
                return I18nManager.getMessage("config.option.disabled");
            }
        }
    }

    private static List<UserConfigOption> createUserOptions() {
        List<UserConfigOption> options = new ArrayList<>();

        options.add(new UserConfigOption(UserConfigOption.OptionType.INTERFACE_LANGUAGE));
        options.add(new UserConfigOption(UserConfigOption.OptionType.SIGN_TRANSLATION));

        return options;
    }

    public ConfigGui(GuiScreen parentScreen) {
        super(parentScreen, createUserOptions(), 2);
    }

    @Override
    protected String getDisplayName(UserConfigOption option) {
        return option.getLabel() + LABEL_STATE_SEPARATOR + option.getStateMessage();
    }

    @Override
    protected void onSelect(UserConfigOption option) {
        playButtonSound();

        try {
            option.toggle();
            refreshList();

            MessageService.sendSuccess(mc.thePlayer,
                    "config.option_changed", option.getLabel(), option.getStateMessage());
        } catch (Exception e) {
            handleConfigError(e, "changement de configuration");
        }
    }

    @Override
    protected List<String> getTooltip(UserConfigOption option) {
        if (option == null) {
            return Collections.emptyList();
        }

        List<String> lines = TooltipManager.buildTooltipLines(option.getLabel(),
                TooltipManager.COLOR_CONTENT + option.getDescription());

        option.getType().appendTooltipHint(lines);

        return lines;
    }

    @Override
    protected String getTitle() {
        return TitleManager.getConfigTitle();
    }
}
