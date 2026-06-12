package fr.ntgitg.mineglot.core.command.export.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.ntgitg.mineglot.core.cache.TranslationCache;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.AIModel;
import fr.ntgitg.mineglot.core.storage.DatabaseOperations;
import fr.ntgitg.mineglot.monitoring.metrics.MetricsManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatsExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String generateReport(String period) {
        JsonObject report = new JsonObject();

        try {
            addReportHeader(report, period);

            addSummarySection(report);

            addEnginesSection(report);

            addCacheSection(report);

            addSystemSection(report);

            addRecommendationsSection(report);

        } catch (Exception e) {
            ModLogger.error("Erreur lors de la generation du rapport", e);

            JsonObject errorReport = new JsonObject();
            errorReport.addProperty("status", "error");
            errorReport.addProperty("message", e.getMessage());
            errorReport.addProperty("timestamp", LocalDateTime.now().toString());
            return GSON.toJson(errorReport);
        }

        return GSON.toJson(report);
    }

    private static void addReportHeader(JsonObject report, String period) {
        report.addProperty("report_type", "MineGlot Personal Usage Statistics");
        report.addProperty("period", period);
        report.addProperty("generated_at",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.addProperty("minecraft_username", Minecraft.getMinecraft().getSession().getUsername());
        report.addProperty("version", "1.0.0");
    }

    private static void addSummarySection(JsonObject report) {
        JsonObject summary = new JsonObject();

        try {
            MetricsManager metrics = MetricsManager.getInstance();

            long totalInputTokens = 0;
            long totalOutputTokens = 0;
            String mostUsedEngine = "none";
            long maxUsage = 0;

            for (AIModel model : AIModel.values()) {
                MetricsManager.ModelStats stats = metrics.getModelStats(model.getModelId());
                totalInputTokens += stats.inputTokens;
                totalOutputTokens += stats.outputTokens;
                long modelUsage = stats.inputTokens + stats.outputTokens;
                if (modelUsage > maxUsage) {
                    maxUsage = modelUsage;
                    mostUsedEngine = model.getEngine();
                }
            }

            summary.addProperty("total_translations", totalInputTokens > 0 ? totalInputTokens / 50 : 0); // Estimation
            summary.addProperty("total_tokens", totalInputTokens + totalOutputTokens);
            summary.addProperty("favorite_engine", mostUsedEngine);
            summary.addProperty("billing_note", "Token totals are informational only. Check your API provider dashboard for real costs.");
            summary.addProperty("cache_efficiency",
                    Math.round(TranslationCache.getInstance().getHitRate() * 10000) / 100.0);

        } catch (Exception e) {
            summary.addProperty("error", "Could not calculate summary: " + e.getMessage());
        }

        report.add("summary", summary);
    }

    private static void addEnginesSection(JsonObject report) {
        JsonObject engines = new JsonObject();

        try {
            MetricsManager metrics = MetricsManager.getInstance();

            for (AIModel model : AIModel.values()) {
                JsonObject engineData = new JsonObject();
                MetricsManager.ModelStats stats = metrics.getModelStats(model.getModelId());

                engineData.addProperty("input_tokens", stats.inputTokens);
                engineData.addProperty("output_tokens", stats.outputTokens);
                engineData.addProperty("total_tokens", stats.inputTokens + stats.outputTokens);

                engineData.addProperty("provider", model.getEngine());

                if (stats.inputTokens + stats.outputTokens > 0) {
                    engineData.addProperty("usage_level",
                            getUsageLevel(stats.inputTokens + stats.outputTokens));
                } else {
                    engineData.addProperty("usage_level", "unused");
                }

                engines.add(model.getModelId(), engineData);
            }
        } catch (Exception e) {
            engines.addProperty("error", "Could not load engine stats: " + e.getMessage());
        }

        report.add("engines", engines);
    }

    private static void addCacheSection(JsonObject report) {
        JsonObject cache = new JsonObject();

        try {
            TranslationCache translationCache = TranslationCache.getInstance();

            cache.addProperty("hit_rate_percent",
                    Math.round(translationCache.getHitRate() * 10000) / 100.0);
            cache.addProperty("total_hits", translationCache.getHitCount());
            cache.addProperty("total_misses", translationCache.getMissCount());
            cache.addProperty("memory_usage_mb",
                    Math.round(translationCache.getMemoryBytes() / 1048576.0 * 100) / 100.0);
            cache.addProperty("total_entries", translationCache.getCacheSize());

            long rocksDbSize = DatabaseOperations.getEstimatedSize();
            if (rocksDbSize >= 0) {
                cache.addProperty("persistent_entries", rocksDbSize);
                cache.addProperty("persistent_entries_estimated", true);
            } else {
                cache.addProperty("persistent_entries", "unavailable");
                cache.addProperty("persistent_entries_estimated", false);
            }

            double hitRate = translationCache.getHitRate();
            String performanceRating;
            if (hitRate >= 0.8) {
                performanceRating = "excellent";
            } else if (hitRate >= 0.6) {
                performanceRating = "good";
            } else if (hitRate >= 0.4) {
                performanceRating = "average";
            } else {
                performanceRating = "poor";
            }
            cache.addProperty("performance_rating", performanceRating);

        } catch (Exception e) {
            cache.addProperty("error", "Could not load cache stats: " + e.getMessage());
        }

        report.add("cache", cache);
    }

    private static void addSystemSection(JsonObject report) {
        JsonObject system = new JsonObject();

        try {
            ConfigurationManager configManager = ConfigurationManager.getInstance();

            system.addProperty("current_model", configManager.getSelectedModel());
            system.addProperty("target_language", configManager.getTargetLanguage());
            system.addProperty("thread_pool_size", configManager.getThreadPoolSize());

            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1048576; // MB
            long freeMemory = runtime.freeMemory() / 1048576; // MB
            long usedMemory = totalMemory - freeMemory;

            system.addProperty("java_memory_total_mb", totalMemory);
            system.addProperty("java_memory_used_mb", usedMemory);
            system.addProperty("java_memory_free_mb", freeMemory);

        } catch (Exception e) {
            system.addProperty("error", "Could not load system info: " + e.getMessage());
        }

        report.add("system", system);
    }

    private static void addRecommendationsSection(JsonObject report) {
        JsonObject recommendations = new JsonObject();

        try {
            double hitRate = TranslationCache.getInstance().getHitRate();
            if (hitRate < 0.5) {
                recommendations.addProperty("cache_tip",
                        "Votre taux de succes cache est faible. Essayez de traduire des textes similaires pour ameliorer l'efficacite.");
            } else if (hitRate > 0.85) {
                recommendations.addProperty("cache_tip", "Excellent! Votre cache est tres efficace.");
            }

            recommendations.addProperty("general_tip",
                    "Pensez a vider le cache via le menu Cache de temps en temps pour liberer la memoire.");
            recommendations.addProperty("billing_tip",
                    "Pour suivre vos depenses, utilisez le tableau de bord du fournisseur associe a votre cle API.");

        } catch (Exception e) {
            recommendations.addProperty("error", "Could not generate recommendations: " + e.getMessage());
        }

        report.add("recommendations", recommendations);
    }

    private static String getUsageLevel(long totalTokens) {
        if (totalTokens > 10000)
            return "heavy";
        if (totalTokens > 1000)
            return "moderate";
        if (totalTokens > 100)
            return "light";
        return "minimal";
    }

}
