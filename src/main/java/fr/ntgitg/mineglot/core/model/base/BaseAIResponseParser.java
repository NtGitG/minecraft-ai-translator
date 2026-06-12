package fr.ntgitg.mineglot.core.model.base;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.monitoring.metrics.MetricsManager;
import fr.ntgitg.mineglot.utils.encoder.LanguageEncoder;
import fr.ntgitg.mineglot.utils.log.ModLogger;

public abstract class BaseAIResponseParser {
    protected static final Gson GSON = new Gson();

    protected BaseAIResponseParser() {
    }

    public String parseTranslation(String responseBody, String originalText, String langCode) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);

            try {
                handleTokenUsage(json);
            } catch (Exception metricsError) {
                ModLogger.debug("Impossible d'extraire les métriques de tokens: {}",
                        metricsError.getMessage());
            }

            String translation = extractTranslation(json);
            if (translation == null || translation.isEmpty()) {
                String snippet = responseBody != null
                        ? responseBody.substring(0, Math.min(responseBody.length(), 1200))
                        : "<null>";
                ModLogger.error("Réponse vide de l'API (snippet: {})", snippet);
                return originalText;
            }

            String decoded = LanguageEncoder.decode(translation, SupportedLanguage.fromCode(langCode));
            return (decoded != null && !decoded.isEmpty()) ? decoded : originalText;

        } catch (Exception e) {
            ModLogger.error("Erreur parsing JSON réponse", e);
            return originalText;
        }
    }

    protected static void handleTokenUsage(JsonObject json) {
        if (json.has("usage")) {
            JsonObject usage = json.getAsJsonObject("usage");
            int input = readInt(usage, "prompt_tokens", "input_tokens");
            int output = readInt(usage, "completion_tokens", "output_tokens");
            MetricsManager.getInstance().addTokensUsed(input, output);
            return;
        }

        if (json.has("usageMetadata")) {
            JsonObject usage = json.getAsJsonObject("usageMetadata");
            int input = readInt(usage, "promptTokenCount");
            int output = readInt(usage, "candidatesTokenCount");
            MetricsManager.getInstance().addTokensUsed(input, output);
        }
    }

    private static int readInt(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.get(key).isJsonNull()) {
                return json.get(key).getAsInt();
            }
        }
        return 0;
    }

    protected abstract String extractTranslation(JsonObject json);
}
