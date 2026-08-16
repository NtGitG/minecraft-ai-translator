package fr.ntgitg.mineglot.monitoring.metrics;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.AIModel;
import fr.ntgitg.mineglot.core.model.token.JsonStatsStorage;
import fr.ntgitg.mineglot.core.model.token.StatsStorage;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class MetricsManager {

    private static final String STATS_FILE_NAME = "model_stats.json";
    private static final long SAVE_DEBOUNCE_MS = 2_000L;

    private final Map<String, AtomicLong> modelInputTokens = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> modelOutputTokens = new ConcurrentHashMap<>();

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    private final StatsStorage storage;

    private MetricsManager() {
        for (AIModel model : AIModel.values()) {
            String modelId = model.getModelId();
            modelInputTokens.put(modelId, new AtomicLong(0));
            modelOutputTokens.put(modelId, new AtomicLong(0));
        }

        this.storage = new JsonStatsStorage(resolveStatsFile());
        loadStats();
    }

    public static MetricsManager getInstance() {
        return SingletonManager.getInstance(MetricsManager.class, MetricsManager::new);
    }

    public void recordModelUsage(String modelId, int inputTokens, int outputTokens) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return;
        }

        AtomicLong inTok = modelInputTokens.computeIfAbsent(modelId, k -> new AtomicLong(0));
        AtomicLong outTok = modelOutputTokens.computeIfAbsent(modelId, k -> new AtomicLong(0));

        inTok.addAndGet(inputTokens);
        outTok.addAndGet(outputTokens);
        scheduleSave();
    }

    public void addTokensUsed(int inputTokens, int outputTokens) {
        try {
            String modelId = ConfigurationManager.getInstance().getSelectedModel();
            if (modelId == null || modelId.trim().isEmpty()) {
                ModLogger.warn("Modele non configure, impossible d'enregistrer les tokens");
                return;
            }

            recordModelUsage(modelId, inputTokens, outputTokens);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'enregistrement des tokens pour inTok={}, outTok={}",
                    inputTokens, outputTokens, e);
        }
    }

    public ModelStats getModelStats(String model) {
        long inputTokens = modelInputTokens.computeIfAbsent(model, k -> new AtomicLong(0)).get();
        long outputTokens = modelOutputTokens.computeIfAbsent(model, k -> new AtomicLong(0)).get();

        return new ModelStats(inputTokens, outputTokens);
    }

    public void resetAllModelStats() {
        for (AIModel model : AIModel.values()) {
            resetModelStats(model.getModelId());
        }
        dirty.set(true);
        flush();
    }

    public void resetTokens() {
        ModLogger.info("Reinitialisation des tokens");
        resetAllModelStats();
    }

    public synchronized void flush() {
        if (!dirty.getAndSet(false)) {
            return;
        }
        saveStatsInternal();
    }

    private void resetModelStats(String model) {
        AtomicLong inTok = modelInputTokens.get(model);
        if (inTok != null) {
            inTok.set(0);
        }

        AtomicLong outTok = modelOutputTokens.get(model);
        if (outTok != null) {
            outTok.set(0);
        }
    }

    private void loadStats() {
        Map<String, StatsStorage.ModelStats> stats = storage.loadStats();
        for (Map.Entry<String, StatsStorage.ModelStats> entry : stats.entrySet()) {
            String modelId = entry.getKey();
            StatsStorage.ModelStats modelStats = entry.getValue();

            AtomicLong inTok = modelInputTokens.get(modelId);
            if (inTok != null) {
                inTok.set(modelStats.getInputTokens());
            }

            AtomicLong outTok = modelOutputTokens.get(modelId);
            if (outTok != null) {
                outTok.set(modelStats.getOutputTokens());
            }
        }
    }

    private synchronized void saveStatsInternal() {
        Map<String, StatsStorage.ModelStats> stats = new HashMap<>();

        for (String modelId : modelInputTokens.keySet()) {
            long inTokens = modelInputTokens.get(modelId).get();
            long outTokens = modelOutputTokens.get(modelId).get();
            stats.put(modelId, new StatsStorage.ModelStats(inTokens, outTokens));
        }

        storage.saveStats(stats);
    }

    private void scheduleSave() {
        dirty.set(true);
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }

        try {
            ThreadManager.getFeatureExecutor().schedule(this::flushScheduledSave,
                    SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            saveScheduled.set(false);
            ModLogger.warn("Planification sauvegarde metrics echouee, flush immediat");
            flush();
        }
    }

    private void flushScheduledSave() {
        saveScheduled.set(false);
        flush();
    }

    private static File resolveStatsFile() {
        try {
            File configDir = Loader.instance().getConfigDir();
            File mineglotDir = new File(configDir, "mineglot");
            if (!mineglotDir.exists() && !mineglotDir.mkdirs()) {
                ModLogger.warn("Impossible de creer le dossier metrics: {}", mineglotDir.getPath());
            }
            return new File(mineglotDir, STATS_FILE_NAME);
        } catch (Exception e) {
            ModLogger.warn("Fallback metrics path utilise");
            return new File("config/mineglot/" + STATS_FILE_NAME);
        }
    }

    public static final class ModelStats {
        public final long inputTokens;
        public final long outputTokens;

        public ModelStats(long inputTokens, long outputTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }
    }
}
