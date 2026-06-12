package fr.ntgitg.mineglot;

import fr.ntgitg.mineglot.core.command.base.CommandManager;
import fr.ntgitg.mineglot.core.config.ConfigService;
import fr.ntgitg.mineglot.core.service.ServiceManager;
import fr.ntgitg.mineglot.core.service.chat.ChatCleanupService;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.system.EventService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.ArrayList;
import java.util.List;

class MineGlotLifecycle {

    void preInit(FMLPreInitializationEvent event) {
        ModLogger.info("=== DEMARRAGE MineGlot v{} ===", MineGlot.VERSION);

        List<String> warnings = new ArrayList<>();
        if (!initializeConfiguration(event)) {
            warnings.add("configuration");
        }
        if (!initializeLanguageManager()) {
            warnings.add("language");
        }

        logPhaseResult("PreInit", warnings);
    }

    void init(FMLInitializationEvent event) {
        ModLogger.info("=== INITIALISATION MineGlot ===");

        List<String> warnings = new ArrayList<>();
        if (!registerEventHandlers()) {
            warnings.add("events");
        }
        if (!registerCommands()) {
            warnings.add("commands");
        }
        if (!startChatCleanupService()) {
            warnings.add("chat-cleanup");
        }

        logPhaseResult("Init", warnings);
    }

    void postInit(FMLPostInitializationEvent event) {
        ModLogger.info("=== POST-INITIALISATION MineGlot ===");

        List<String> warnings = new ArrayList<>();
        if (!initializeServices()) {
            warnings.add("services");
        }
        if (!saveConfiguration()) {
            warnings.add("config-save");
        }
        if (!validateServices()) {
            warnings.add("service-validation");
        }

        setupShutdownHook();
        logPhaseResult("PostInit", warnings);
    }

    private void logPhaseResult(String phase, List<String> warnings) {
        if (warnings.isEmpty()) {
            ModLogger.info("{} termine avec succes", phase);
            return;
        }
        ModLogger.warn("{} termine avec avertissements: {}", phase, String.join(", ", warnings));
    }

    private boolean initializeConfiguration(FMLPreInitializationEvent event) {
        return runStep("initialisation configuration", () -> {
            ConfigService configService = ConfigService.getInstance();
            return configService.preparePreInitialization(event.getSuggestedConfigurationFile());
        });
    }

    private boolean initializeLanguageManager() {
        return runStep("initialisation language manager", () -> {
            String currentLang = I18nManager.getCurrentLanguage();
            if (currentLang == null || currentLang.isEmpty()) {
                I18nManager.initialize();
                currentLang = I18nManager.getCurrentLanguage();
            }

            if (currentLang == null || currentLang.isEmpty()) {
                ModLogger.warn("Language manager non initialise, fallback anglais");
                return false;
            }

            String testMessage = I18nManager.getMessage("mod.name");
            if (testMessage == null || testMessage.isEmpty() || "mod.name".equals(testMessage)) {
                ModLogger.warn("Messages i18n de base non charges");
            }

            ModLogger.info("Language manager pret ({})", currentLang);
            return true;
        });
    }

    private boolean registerEventHandlers() {
        return runStep("enregistrement des evenements", () -> {
            EventService.getInstance().start();
            return true;
        });
    }

    private boolean registerCommands() {
        return runStep("enregistrement des commandes", () -> {
            CommandManager.getInstance().initializeCommands();
            return true;
        });
    }

    private boolean startChatCleanupService() {
        return runStep("demarrage nettoyage chat", () -> {
            ChatCleanupService.getInstance().start();
            return true;
        });
    }

    private boolean initializeServices() {
        return runStep("initialisation des services", () -> {
            ServiceManager.getInstance().initializeServices();
            return true;
        });
    }

    private boolean saveConfiguration() {
        return runStep("sauvegarde configuration", () -> {
            ConfigService configService = ServiceManager.getInstance().getService(ConfigService.class);
            if (configService == null || !configService.isOperational()) {
                return false;
            }
            return configService.saveIfChanged();
        });
    }

    private boolean validateServices() {
        return runStep("validation des services", () -> {
            I18nManager.syncWithConfig();
            return ServiceManager.getInstance().areAllServicesOperational();
        });
    }

    private void setupShutdownHook() {
        runStep("configuration du shutdown hook", () -> {
            ClientShutdownManager.getInstance().installShutdownHook();
            return true;
        });
    }

    private boolean runStep(String stepName, StepAction action) {
        try {
            return action.run();
        } catch (Exception e) {
            ModLogger.error("Erreur lors de {}", stepName, e);
            return false;
        }
    }

    @FunctionalInterface
    private interface StepAction {
        boolean run() throws Exception;
    }
}
