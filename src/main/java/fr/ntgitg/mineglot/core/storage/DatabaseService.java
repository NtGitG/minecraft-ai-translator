package fr.ntgitg.mineglot.core.storage;

import fr.ntgitg.mineglot.core.config.ConfigPathResolver;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.AbstractService;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.core.service.error.ErrorTechniques;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseService extends AbstractService {
    private static final long CACHE_ENTRY_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7);
    private static final long CLEANUP_RUN_INTERVAL_MS = TimeUnit.DAYS.toMillis(1);
    private static final long STARTUP_CLEANUP_DELAY_SECONDS = 30L;

    private final ConfigurationManager configManager;
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    private volatile ScheduledFuture<?> cleanupTask;

    DatabaseService() {
        super("Database");
        this.configManager = ConfigurationManager.getInstance();
    }

    public static DatabaseService getInstance() {
        return SingletonManager.getInstance(DatabaseService.class, DatabaseService::new);
    }

    @Override
    protected void doStart() throws Exception {
        stopping.set(false);

        String dbPath = configManager.getDbPath();
        if (!ValidationService.isValidDbPathSimple(dbPath)) {
            dbPath = ConfigPathResolver.getDefaultDbPath();
        }

        java.io.File dbDir = new java.io.File(dbPath);
        if (!dbDir.exists() && !dbDir.mkdirs()) {
            throw new IllegalStateException("Impossible de creer le dossier RocksDB: " + dbPath);
        }
        if (!dbDir.isDirectory()) {
            throw new IllegalStateException("Chemin RocksDB invalide (pas un dossier): " + dbPath);
        }

        final String resolvedDbPath = dbPath;
        ErrorTechniques.executeWithErrorHandling(() -> {
            DatabaseOperations.configureDbPath(resolvedDbPath);
            DatabaseOperations.initAsync();
        }, "initialisation RocksDB");

        scheduleOldCacheCleanup();
    }

    @Override
    protected void doStop() throws Exception {
        stopping.set(true);
        cancelOldCacheCleanup();
        DatabaseOperations.close();
    }

    private void scheduleOldCacheCleanup() {
        try {
            cleanupTask = ThreadManager.getFeatureExecutor().schedule(() -> {
                if (stopping.get() || ThreadManager.isShutdown()) {
                    return;
                }

                ThreadManager.runDbAsync(() -> {
                    if (stopping.get() || ThreadManager.isShutdown()) {
                        return;
                    }
                    TranslationStorage.cleanupOldEntriesIfDue(CACHE_ENTRY_MAX_AGE_MS,
                            CLEANUP_RUN_INTERVAL_MS,
                            () -> stopping.get() || ThreadManager.isShutdown());
                }).exceptionally(error -> {
                    ModLogger.warn("Nettoyage asynchrone du cache ignore", error);
                    return null;
                });
            }, STARTUP_CLEANUP_DELAY_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            ModLogger.warn("Planification du nettoyage asynchrone du cache impossible", e);
        }
    }

    private void cancelOldCacheCleanup() {
        ScheduledFuture<?> task = cleanupTask;
        cleanupTask = null;
        if (task != null) {
            task.cancel(false);
        }
    }
}
