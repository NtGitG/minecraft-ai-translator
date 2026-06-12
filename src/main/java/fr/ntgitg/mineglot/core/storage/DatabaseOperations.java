package fr.ntgitg.mineglot.core.storage;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.service.error.ErrorTechniques;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.CompressionType;
import org.rocksdb.FlushOptions;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class DatabaseOperations {

    private enum DbState {
        NOT_STARTED,
        STARTING,
        READY,
        FAILED
    }

    private static final int BLOCK_CACHE_SIZE_MB = 32;
    private static final int WRITE_BUFFER_SIZE_MB = 64;
    private static final int TARGET_FILE_SIZE_MB = 64;
    private static final int MAX_BYTES_FOR_LEVEL_BASE_MB = 256;
    private static final int MAX_BACKGROUND_JOBS = 4;
    private static final int MAX_WRITE_BUFFER_NUMBER = 4;
    private static final int MAX_OPEN_FILES = 300;
    private static final int BLOOM_FILTER_BITS = 10;
    private static final String ESTIMATE_NUM_KEYS_PROPERTY = "rocksdb.estimate-num-keys";

    private final AtomicReference<DbState> state = new AtomicReference<>(DbState.NOT_STARTED);
    private final AtomicBoolean initScheduled = new AtomicBoolean(false);
    private final AtomicBoolean notReadyLogged = new AtomicBoolean(false);
    private final ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock();

    private volatile RocksDB db;
    private volatile Options options;
    private volatile String configuredDbPath;

    private DatabaseOperations() {
    }

    private static DatabaseOperations getInstance() {
        return SingletonManager.getInstance(DatabaseOperations.class, DatabaseOperations::new);
    }

    public static void init() {
        getInstance().initInternal();
    }

    public static void initAsync() {
        getInstance().initAsyncInternal();
    }

    public static void cleanup() {
        getInstance().cleanupInternal();
    }

    public static void close() {
        cleanup();
    }

    public static void configureDbPath(String dbPath) {
        getInstance().configureDbPathInternal(dbPath);
    }

    public static String get(String key) {
        return getInstance().getStringInternal(key);
    }

    public static void put(String key, String value) {
        getInstance().putStringInternal(key, value);
    }

    public static void delete(String key) {
        getInstance().deleteStringInternal(key);
    }

    public static void clear() {
        getInstance().clearInternal();
    }

    public static long getSize() {
        return getInstance().getSizeInternal();
    }

    public static long getEstimatedSize() {
        return getInstance().getEstimatedSizeInternal();
    }

    public static void forEachEntry(BiConsumer<byte[], byte[]> consumer) {
        getInstance().forEachEntryInternal(consumer);
    }

    public static boolean forEachEntryWhile(BiPredicate<byte[], byte[]> consumer) {
        return getInstance().forEachEntryWhileInternal(consumer);
    }

    public static void writeBatch(WriteBatch batch) {
        getInstance().writeBatchInternal(batch);
    }

    private void configureDbPathInternal(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return;
        }

        Lock writeLock = dbLock.writeLock();
        writeLock.lock();
        try {
            if (state.get() == DbState.READY || state.get() == DbState.STARTING) {
                ModLogger.debug("RocksDB deja en cours/pret, changement de chemin ignore: {}", dbPath);
                return;
            }
            configuredDbPath = dbPath.trim();
        } finally {
            writeLock.unlock();
        }
    }

    private void initInternal() {
        Lock writeLock = dbLock.writeLock();
        writeLock.lock();
        try {
            DbState current = state.get();
            if (current == DbState.READY || current == DbState.STARTING) {
                return;
            }

            try {
                state.set(DbState.STARTING);
                RocksDB.loadLibrary();

                if (options == null) {
                    options = createOptimizedOptions();
                }

                String dbPath = resolveDbPath();
                File dbDir = new File(dbPath);
                if (!dbDir.exists() && !dbDir.mkdirs()) {
                    throw new IllegalStateException("Impossible de creer le dossier RocksDB: " + dbPath);
                }
                if (!dbDir.isDirectory()) {
                    throw new IllegalStateException("Chemin RocksDB invalide (pas un dossier): " + dbPath);
                }
                db = RocksDB.open(options, dbPath);

                state.set(DbState.READY);
                notReadyLogged.set(false);
                ModLogger.info("Base de donnees RocksDB initialisee avec succes : {}", dbPath);
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'initialisation de RocksDB", e);
                state.set(DbState.FAILED);
                db = null;

                if (options != null) {
                    options.close();
                    options = null;
                }

                ModLogger.warn("MineGlot continuera sans persistance RocksDB");
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void initAsyncInternal() {
        DbState current = state.get();
        if (current == DbState.READY || current == DbState.STARTING) {
            return;
        }

        if (!initScheduled.compareAndSet(false, true)) {
            return;
        }

        ModLogger.info("Initialisation RocksDB en arriere-plan...");
        ThreadManager.runDbAsync(() -> {
            try {
                initInternal();
            } finally {
                initScheduled.set(false);
            }
        });
    }

    private String resolveDbPath() {
        if (configuredDbPath != null && !configuredDbPath.isEmpty()) {
            return configuredDbPath;
        }
        return ConfigurationManager.getInstance().getDbPath();
    }

    @SuppressWarnings("deprecation")
    private static Options createOptimizedOptions() {
        Options created = new Options();
        created.setCreateIfMissing(true);
        created.setMaxOpenFiles(MAX_OPEN_FILES);
        created.setIncreaseParallelism(Runtime.getRuntime().availableProcessors());

        created.setMaxBackgroundJobs(MAX_BACKGROUND_JOBS);
        created.setMaxWriteBufferNumber(MAX_WRITE_BUFFER_NUMBER);
        created.setWriteBufferSize(WRITE_BUFFER_SIZE_MB * 1024L * 1024L);
        created.setTargetFileSizeBase(TARGET_FILE_SIZE_MB * 1024L * 1024L);
        created.setMaxBytesForLevelBase(MAX_BYTES_FOR_LEVEL_BASE_MB * 1024L * 1024L);

        created.setCompressionType(CompressionType.LZ4_COMPRESSION);
        created.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);

        BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
        tableConfig.setBlockCacheSize(BLOCK_CACHE_SIZE_MB * 1024L * 1024L);
        tableConfig.setCacheIndexAndFilterBlocks(true);
        tableConfig.setPinTopLevelIndexAndFilter(true);
        tableConfig.setCacheIndexAndFilterBlocksWithHighPriority(true);

        try {
            BloomFilter bloomFilter = new BloomFilter(BLOOM_FILTER_BITS, false);
            tableConfig.setFilter(bloomFilter);
        } catch (Exception ignored) {
        }

        created.setTableFormatConfig(tableConfig);
        created.setMaxSubcompactions(2);
        created.setLevelCompactionDynamicLevelBytes(true);
        created.setOptimizeFiltersForHits(true);
        created.setLevelZeroFileNumCompactionTrigger(4);
        created.setAdviseRandomOnOpen(true);

        return created;
    }

    private void cleanupInternal() {
        Lock writeLock = dbLock.writeLock();
        writeLock.lock();
        try {
            if (db != null) {
                try (FlushOptions flushOptions = new FlushOptions()) {
                    flushOptions.setWaitForFlush(true);
                    db.flush(flushOptions);
                } catch (Exception e) {
                    ModLogger.error("Erreur lors du flush RocksDB", e);
                } finally {
                    db.close();
                    db = null;
                    ModLogger.info("RocksDB correctement ferme.");
                }
            }

            if (options != null) {
                options.close();
                options = null;
            }

            state.set(DbState.NOT_STARTED);
            notReadyLogged.set(false);
            initScheduled.set(false);
        } finally {
            writeLock.unlock();
        }
    }

    private String getStringInternal(String key) {
        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return null;
            }

            try (ReadOptions readOptions = new ReadOptions()) {
                byte[] raw = db.get(readOptions, key.getBytes(StandardCharsets.UTF_8));
                return raw != null ? new String(raw, StandardCharsets.UTF_8) : null;
            } catch (Exception e) {
                ModLogger.error("Erreur de lecture dans RocksDB", e);
                ErrorTechniques.handleAndLog(e, "lecture dans la base de donnees");
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    private void putStringInternal(String key, String value) {
        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return;
            }

            try (WriteOptions writeOptions = new WriteOptions()) {
                db.put(writeOptions, key.getBytes(StandardCharsets.UTF_8),
                        value.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                ModLogger.error("Erreur d'ecriture dans RocksDB", e);
                ErrorTechniques.handleAndLog(e, "ecriture dans la base de donnees");
            }
        } finally {
            readLock.unlock();
        }
    }

    private void deleteStringInternal(String key) {
        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return;
            }

            try (WriteOptions writeOptions = new WriteOptions()) {
                db.delete(writeOptions, key.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                ModLogger.error("Erreur de suppression dans RocksDB", e);
                ErrorTechniques.handleAndLog(e, "suppression dans la base de donnees");
            }
        } finally {
            readLock.unlock();
        }
    }

    private void clearInternal() {
        Lock writeLock = dbLock.writeLock();
        writeLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return;
            }

            clearWithWriteBatchLocked();
            try {
                db.compactRange();
            } catch (Exception e) {
                ModLogger.debug("Compactage RocksDB ignore apres clear: {}", e.getMessage());
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void clearWithWriteBatchLocked() {
        try (WriteBatch batch = new WriteBatch();
             WriteOptions writeOptions = new WriteOptions();
             ReadOptions readOptions = new ReadOptions()) {
            try (RocksIterator it = db.newIterator(readOptions)) {
                for (it.seekToFirst(); it.isValid(); it.next()) {
                    batch.delete(it.key());
                }
            }
            db.write(writeOptions, batch);
            ModLogger.info("Cache RocksDB vide avec WriteBatch");
        } catch (Exception e) {
            ModLogger.error("Erreur de nettoyage du cache avec WriteBatch", e);
            ErrorTechniques.handleAndLog(e, "nettoyage du cache RocksDB");
        }
    }

    private long getSizeInternal() {
        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return 0;
            }

            long estimatedSize = getEstimatedSizeLocked();
            if (estimatedSize >= 0) {
                return estimatedSize;
            }

            return countEntriesLocked();
        } finally {
            readLock.unlock();
        }
    }

    private long getEstimatedSizeInternal() {
        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return -1;
            }

            return getEstimatedSizeLocked();
        } finally {
            readLock.unlock();
        }
    }

    private long getEstimatedSizeLocked() {
        try {
            return db.getLongProperty(ESTIMATE_NUM_KEYS_PROPERTY);
        } catch (Exception e) {
            ModLogger.debug("Estimation RocksDB indisponible: {}", e.getMessage());
            return -1;
        }
    }

    private long countEntriesLocked() {
        long count = 0;
        try (ReadOptions readOptions = new ReadOptions();
             RocksIterator it = db.newIterator(readOptions)) {
            for (it.seekToFirst(); it.isValid(); it.next()) {
                count++;
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors du comptage des entrees RocksDB", e);
            ErrorTechniques.handleAndLog(e, "comptage des entrees RocksDB");
        }
        return count;
    }

    private void forEachEntryInternal(BiConsumer<byte[], byte[]> consumer) {
        if (consumer == null) {
            return;
        }

        forEachEntryWhileInternal((key, value) -> {
            consumer.accept(key, value);
            return true;
        });
    }

    private boolean forEachEntryWhileInternal(BiPredicate<byte[], byte[]> consumer) {
        if (consumer == null) {
            return true;
        }

        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return false;
            }

            try (ReadOptions readOptions = new ReadOptions();
                 RocksIterator it = db.newIterator(readOptions)) {
                for (it.seekToFirst(); it.isValid(); it.next()) {
                    if (!consumer.test(it.key(), it.value())) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                ModLogger.error("Erreur lors du parcours des entrees RocksDB", e);
                ErrorTechniques.handleAndLog(e, "parcours des entrees RocksDB");
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    private void writeBatchInternal(WriteBatch batch) {
        if (batch == null) {
            return;
        }

        Lock readLock = dbLock.readLock();
        readLock.lock();
        try {
            if (!ensureReadyLocked()) {
                return;
            }

            try (WriteOptions writeOptions = new WriteOptions()) {
                db.write(writeOptions, batch);
            } catch (Exception e) {
                ModLogger.error("Erreur lors de l'application du batch RocksDB", e);
                ErrorTechniques.handleAndLog(e, "application du batch RocksDB");
            }
        } finally {
            readLock.unlock();
        }
    }

    private boolean ensureReadyLocked() {
        DbState current = state.get();
        if (current == DbState.READY && db != null) {
            return true;
        }

        if (current == DbState.READY) {
            state.set(DbState.FAILED);
            logNotReady(DbState.FAILED);
            return false;
        }

        if (current == DbState.NOT_STARTED) {
            initAsyncInternal();
            logNotReady(current);
            return false;
        }

        logNotReady(current);
        return false;
    }

    private void logNotReady(DbState current) {
        if (!notReadyLogged.compareAndSet(false, true)) {
            return;
        }

        if (current == DbState.FAILED) {
            ModLogger.warn("RocksDB indisponible (etat: FAILED). Mode memoire uniquement.");
        } else {
            ModLogger.info("RocksDB pas encore pret (etat: {}). Mode memoire uniquement.", current);
        }
    }
}
