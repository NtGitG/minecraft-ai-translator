package fr.ntgitg.mineglot.core.model.token;

import com.google.gson.*;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonStatsStorage implements StatsStorage {
    private final File statsFile;
    private final Gson gson;

    public JsonStatsStorage(File statsFile) {
        this.statsFile = statsFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public Map<String, ModelStats> loadStats() {
        if (!statsFile.exists()) {
            ModLogger.info(
                    "Aucun fichier de stats trouvé (" + statsFile.getPath() + "), initialisation vide.");
            return new HashMap<>();
        }

        try (FileReader reader = new FileReader(statsFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                ModLogger.warn("Le fichier de stats est vide ou invalide (JSON null), réinitialisation.");
                return new HashMap<>();
            }

            Map<String, ModelStats> stats = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String modelId = entry.getKey();
                JsonObject modelStats = entry.getValue().getAsJsonObject();

                long inputTokens = 0;
                long outputTokens = 0;

                if (modelStats.has("inputTokens") && !modelStats.get("inputTokens").isJsonNull()) {
                    inputTokens = modelStats.get("inputTokens").getAsLong();
                }
                if (modelStats.has("outputTokens") && !modelStats.get("outputTokens").isJsonNull()) {
                    outputTokens = modelStats.get("outputTokens").getAsLong();
                }

                stats.put(modelId, new ModelStats(inputTokens, outputTokens));
            }
            return stats;

        } catch (JsonSyntaxException jse) {
            ModLogger.warn(
                    "Le JSON de stats est corrompu, suppression et réinitialisation : " + jse.getMessage());
            return new HashMap<>();
        } catch (IOException ioe) {
            ModLogger.error("Erreur I/O lors du chargement des stats", ioe);
            return new HashMap<>();
        } catch (Exception e) {
            ModLogger.error("Erreur inattendue lors du chargement des stats", e);
            return new HashMap<>();
        }
    }

    @Override
    public void saveStats(Map<String, ModelStats> stats) {
        try {
            File parentDir = statsFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    ModLogger.warn("Impossible de créer le répertoire parent pour " + statsFile.getPath());
                }
            }

            File tmpFile = new File(parentDir, statsFile.getName() + ".tmp");
            try (FileWriter writer = new FileWriter(tmpFile)) {
                gson.toJson(stats, writer);
            }

            if (statsFile.exists()) {
                if (!statsFile.delete()) {
                    ModLogger
                            .warn("Impossible de supprimer l'ancien fichier de stats : " + statsFile.getPath());
                }
            }
            if (!tmpFile.renameTo(statsFile)) {
                ModLogger
                        .error("Impossible de renommer " + tmpFile.getPath() + " en " + statsFile.getPath());
            }
        } catch (IOException ioe) {
            ModLogger.error("Erreur I/O lors de la sauvegarde des stats", ioe);
        } catch (Exception e) {
            ModLogger.error("Erreur inattendue lors de la sauvegarde des stats", e);
        }
    }
}
