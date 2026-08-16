package fr.ntgitg.mineglot.core.service;

import fr.ntgitg.mineglot.core.cache.CacheServiceFacade;
import fr.ntgitg.mineglot.core.config.ConfigService;
import fr.ntgitg.mineglot.core.service.lingua.LinguaLanguageService;
import fr.ntgitg.mineglot.core.service.system.EventService;
import fr.ntgitg.mineglot.core.storage.DatabaseService;
import fr.ntgitg.mineglot.core.translation.TranslationService;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceManager {

    private static final Set<Class<? extends Service>> CRITICAL_SERVICES =
            Collections.unmodifiableSet(new HashSet<>(
                    Arrays.asList(
                            ConfigService.class
                    )
            ));

    private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();
    private final List<Service> serviceOrder = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean initialized;

    private ServiceManager() {
        this.initialized = false;
    }

    public static ServiceManager getInstance() {
        return SingletonManager.getInstance(ServiceManager.class, ServiceManager::new);
    }

    public void initializeServices() {
        synchronized (this) {
            if (initialized) {
                ModLogger.debug("Services deja initialises");
                return;
            }

            ModLogger.info("Initialisation des services...");
            try {
                registerServices();
                initialized = true;
                ModLogger.info("Services enregistres avec succes");
            } catch (Exception ex) {
                ModLogger.error("Echec de l'enregistrement des services", ex);
                cleanup();
                throw new IllegalStateException("Service registration failed", ex);
            }
        }

        startServices();
        logAsyncServicesStatus();
        ModLogger.info("Initialisation des services terminee");
    }

    public void restartServices(boolean forceRestart) {
        if (!initialized && !forceRestart) {
            ModLogger.warn("Services non initialises - redemarrage ignore");
            return;
        }

        ModLogger.info("Redemarrage des services...");
        try {
            stopServices();
            initializeServices();
            ModLogger.info("Services redemarres avec succes");
        } catch (Exception e) {
            ModLogger.error("Echec du redemarrage des services", e);
            throw new IllegalStateException("Service restart failed", e);
        }
    }

    public void restartService(Class<? extends Service> serviceClass) {
        Service service = getService(serviceClass);
        if (service == null) {
            throw new IllegalArgumentException("Service non trouve: "
                    + serviceClass.getSimpleName());
        }

        ModLogger.info("Redemarrage du service: {}", service.getName());

        try {
            service.stop();
            service.start();

            if (service.isOperational()) {
                ModLogger.info("Service redemarre: {}", service.getName());
            } else {
                ModLogger.warn("Service redemarre mais non operationnel: {}", service.getName());
            }
        } catch (Exception e) {
            ModLogger.error("Echec du redemarrage du service: {}", service.getName(), e);
            throw new IllegalStateException("Service restart failed: " + service.getName(), e);
        }
    }

    @SafeVarargs
    public final void restartServices(Class<? extends Service>... serviceClasses) {
        if (serviceClasses == null || serviceClasses.length == 0) {
            ModLogger.warn("Aucun service specifie pour le redemarrage");
            return;
        }

        ModLogger.info("Redemarrage de {} services...", serviceClasses.length);

        for (Class<? extends Service> serviceClass : serviceClasses) {
            try {
                restartService(serviceClass);
            } catch (Exception e) {
                if (CRITICAL_SERVICES.contains(serviceClass)) {
                    ModLogger.error("Service critique echoue: {}",
                            serviceClass.getSimpleName(), e);
                    throw new IllegalStateException(
                            "Critical service restart failed: " + serviceClass.getSimpleName(), e);
                }

                ModLogger.error("Service non-critique echoue: {}",
                        serviceClass.getSimpleName(), e);
            }
        }

        ModLogger.info("Redemarrage des services termine");
    }

    private void registerServices() {
        registerService(ConfigService.getInstance());
        registerService(DatabaseService.getInstance());
        registerService(CacheServiceFacade.getInstance());
        registerService(TranslationService.getInstance());
        registerService(EventService.getInstance());
        registerService(LinguaLanguageService.getInstance());
    }

    void registerService(Service service) {
        ValidationService.ValidationResult result = validateServiceForRegistration(service);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getErrorMessage());
        }

        Service existing = services.putIfAbsent(service.getClass(), service);
        if (existing == null) {
            serviceOrder.add(service);
        } else {
            ModLogger.warn("Service deja enregistre: {}", service.getClass().getSimpleName());
        }
    }

    private ValidationService.ValidationResult validateServiceForRegistration(Service service) {
        if (service == null) {
            return ValidationService.ValidationResult.error("service null", "service.null");
        }
        if (!ValidationService.isNotEmpty(service.getName())) {
            return ValidationService.ValidationResult.error("service name empty", "service.empty");
        }
        if (services.containsKey(service.getClass())) {
            return ValidationService.ValidationResult.error("already registered", "service.dup");
        }
        return ValidationService.ValidationResult.success();
    }

    private void startServices() {
        for (Service service : snapshotServiceOrder()) {
            Class<? extends Service> serviceClass = service.getClass();
            try {
                service.start();
                if (!service.isOperational()) {
                    if (service instanceof LinguaLanguageService
                            && ((LinguaLanguageService) service).isInitializing()) {
                        ModLogger.info("Service {} en initialisation asynchrone",
                                service.getName());
                    } else {
                        ModLogger.warn("Service demarre mais non operationnel: {}",
                                service.getName());
                    }
                }
            } catch (Exception ex) {
                if (CRITICAL_SERVICES.contains(serviceClass)) {
                    ModLogger.error("Service critique echoue: {}", service.getName(), ex);
                    throw new IllegalStateException("Critical service failed", ex);
                }

                ModLogger.error("Erreur lors du demarrage du service: {}", service.getName(), ex);
            }
        }
    }

    private void logAsyncServicesStatus() {
        LinguaLanguageService languageService = getService(LinguaLanguageService.class);
        if (languageService == null || languageService.isLanguageDetectorReady()) {
            return;
        }

        ModLogger.info("LanguageService continue son initialisation en arriere-plan; "
                + "la detection utilisera 'auto' jusqu'a ce que Lingua soit pret");
    }

    public void stopServices() {
        synchronized (this) {
            if (!initialized) {
                ModLogger.debug("Services deja arretes");
                return;
            }

            ModLogger.info("Arret des services...");

            List<Service> reversedOrder = snapshotServiceOrder();
            Collections.reverse(reversedOrder);

            for (Service service : reversedOrder) {
                try {
                    ModLogger.info("Arret du service: {}", service.getName());
                    service.stop();
                } catch (Exception ex) {
                    ModLogger.error("Erreur lors de l'arret du service: {}", service.getName(), ex);
                }
            }

            cleanup();
            ModLogger.info("Tous les services ont ete arretes");
        }
    }

    private void cleanup() {
        services.clear();
        synchronized (serviceOrder) {
            serviceOrder.clear();
        }
        initialized = false;
    }

    public <T extends Service> T getService(Class<T> serviceClass) {
        return serviceClass == null ? null : serviceClass.cast(services.get(serviceClass));
    }

    public boolean isServiceOperational(Class<? extends Service> serviceClass) {
        Service service = services.get(serviceClass);
        return service != null && service.isOperational();
    }

    public boolean areAllServicesOperational() {
        return services.values().stream().allMatch(service ->
                service.isOperational() || isAllowedAsyncInitialization(service));
    }

    public int getServiceCount() {
        return services.size();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isCriticalServiceOperational(Class<? extends Service> serviceClass) {
        return CRITICAL_SERVICES.contains(serviceClass) && isServiceOperational(serviceClass);
    }

    public Set<Class<? extends Service>> getCriticalServices() {
        return new HashSet<>(CRITICAL_SERVICES);
    }

    private List<Service> snapshotServiceOrder() {
        synchronized (serviceOrder) {
            return new ArrayList<>(serviceOrder);
        }
    }

    private boolean isAllowedAsyncInitialization(Service service) {
        return service instanceof LinguaLanguageService
                && ((LinguaLanguageService) service).isInitializing();
    }
}
