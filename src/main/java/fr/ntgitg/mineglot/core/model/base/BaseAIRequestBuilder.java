package fr.ntgitg.mineglot.core.model.base;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.utils.log.ModLogger;

public final class BaseAIRequestBuilder {

    // Prompts courts pour limiter le coût token et éviter les réponses type chatbot
    private static final String SYSTEM_PROMPT_AUTO = "Translate into %s. If already %s, return unchanged. No questions. Reply with translation only.";

    private static final String SYSTEM_PROMPT_KNOWN = "Source is %s. Translate into %s. If already %s, return unchanged. No questions. Reply with translation only.";

    private BaseAIRequestBuilder() {
    }

    /**
     * Build a payload for Anthropic's Messages API.
     *
     * <p>Anthropic expects the system prompt at the request root rather than as a message.
     * Sampling temperature is intentionally omitted because current Opus models reject
     * non-default sampling parameters.</p>
     */
    public static String buildClaudeRequestBody(String preparedText, String sourceLang,
                                                String targetLang, String model, int maxTokens) {
        String finalPrompt = buildSystemPrompt(sourceLang, targetLang);

        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        json.addProperty("max_tokens", maxTokens);
        json.addProperty("system", finalPrompt);

        JsonArray messages = new JsonArray();

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", preparedText);
        messages.add(userMsg);

        json.add("messages", messages);

        ModLogger.debug("=== ENVOI API ===");
        ModLogger.debug("Langue source: {}, cible: {}, modele: {}, longueur: {}", sourceLang,
                targetLang, model, preparedText.length());
        ModLogger.debug("Message systeme : {}", finalPrompt);
        ModLogger.debug("Message utilisateur : {}", preparedText);
        ModLogger.debug("Corps de la requete : {}", json.toString());
        ModLogger.debug("=== FIN ENVOI ===");

        return json.toString();
    }

    /**
     * Build payload for OpenAI Responses API (preferred endpoint).
     */
    public static String buildOpenAIResponsesRequestBody(String preparedText, String sourceLang,
                                                         String targetLang, String model,
                                                         int maxOutputTokens, double temperature) {
        String finalPrompt = buildSystemPrompt(sourceLang, targetLang);

        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        json.addProperty("instructions", finalPrompt);
        json.add("input", buildResponsesInput(preparedText));
        json.addProperty("max_output_tokens", maxOutputTokens);
        json.addProperty("temperature", temperature);
        json.addProperty("store", false);

        ModLogger.debug("=== ENVOI OPENAI RESPONSES ===");
        ModLogger.debug("Langue source: {}, cible: {}, modele: {}, longueur: {}", sourceLang,
                targetLang, model, preparedText.length());
        ModLogger.debug("Instructions : {}", finalPrompt);
        ModLogger.debug("Input : {}", preparedText);
        ModLogger.debug("Corps de la requete : {}", json.toString());
        ModLogger.debug("=== FIN ENVOI ===");

        return json.toString();
    }

    private static JsonArray buildResponsesInput(String preparedText) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "input_text");
        textPart.addProperty("text", preparedText);

        JsonArray content = new JsonArray();
        content.add(textPart);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.add("content", content);

        JsonArray input = new JsonArray();
        input.add(user);
        return input;
    }

    private static String buildSystemPrompt(String sourceLang, String targetLang) {
        SupportedLanguage targetLanguage = SupportedLanguage.fromCode(targetLang);
        if (targetLanguage == null || "auto".equalsIgnoreCase(targetLang)) {
            targetLanguage = SupportedLanguage.ENGLISH;
        }
        String targetEnglishName = targetLanguage.getEnglishName();

        if ("auto".equalsIgnoreCase(sourceLang)) {
            return String.format(SYSTEM_PROMPT_AUTO, targetEnglishName, targetEnglishName);
        }

        SupportedLanguage sourceLanguage = SupportedLanguage.fromCode(sourceLang);
        String sourceEnglishName = sourceLanguage != null ? sourceLanguage.getEnglishName() : "Unknown";
        return String.format(SYSTEM_PROMPT_KNOWN, sourceEnglishName, targetEnglishName,
                targetEnglishName);
    }
}
