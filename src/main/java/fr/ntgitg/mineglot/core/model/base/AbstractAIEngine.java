package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.model.claude.ClaudeResponseParser;
import fr.ntgitg.mineglot.core.model.openai.OpenAIResponseParser;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AbstractAIEngine {

    private static final int MAX_TOKENS = 1000;
    private static final double TEMPERATURE = 0.7;

    private interface RequestBodyBuilder {
        String build(String text, String sourceLang, String targetLang);
    }

    private interface JsonPoster {
        String post(String payload, String apiKey) throws IOException;
    }

    private interface TranslationParser {
        String parse(String response, String originalText, String targetLang);
    }

    private interface ApiUrlProvider {
        String getApiUrl();
    }

    private interface HeaderConfigurer {
        void configure(HttpPost post, String apiKey);
    }

    private static final class EngineBridge {
        private final ApiKeyValidator validator;
        private final RequestBodyBuilder requestBuilder;
        private final JsonPoster httpPoster;
        private final TranslationParser responseParser;

        private EngineBridge(ApiKeyValidator validator, RequestBodyBuilder requestBuilder,
                             JsonPoster httpPoster, TranslationParser responseParser) {
            this.validator = validator;
            this.requestBuilder = requestBuilder;
            this.httpPoster = httpPoster;
            this.responseParser = responseParser;
        }
    }

    private static final class TranslationRequest {
        private final EngineBridge bridge;
        private final String apiKey;

        private TranslationRequest(EngineBridge bridge, String apiKey) {
            this.bridge = bridge;
            this.apiKey = apiKey;
        }
    }

    private static final class EngineHttpClient extends BaseHttpClient {
        private final ApiUrlProvider apiUrlProvider;
        private final HeaderConfigurer headerConfigurer;

        private EngineHttpClient(ApiUrlProvider apiUrlProvider,
                                 HeaderConfigurer headerConfigurer) {
            this.apiUrlProvider = apiUrlProvider;
            this.headerConfigurer = headerConfigurer;
        }

        @Override
        protected String getApiUrl() {
            return apiUrlProvider.getApiUrl();
        }

        @Override
        protected void addHeaders(HttpPost post, String apiKey) {
            headerConfigurer.configure(post, apiKey);
        }
    }

    private static final BaseHttpClient OPENAI_HTTP_CLIENT = new EngineHttpClient(
            () -> ProviderApiEndpoints.OPENAI_RESPONSES_URL,
            (post, apiKey) -> {
                post.setHeader("Authorization", "Bearer " + apiKey);
                post.setHeader("Accept-Encoding", "gzip");
            });

    private static final BaseHttpClient CLAUDE_HTTP_CLIENT = new EngineHttpClient(
            () -> ProviderApiEndpoints.CLAUDE_MESSAGES_URL,
            (post, apiKey) -> {
                post.setHeader("x-api-key", apiKey);
                post.setHeader("anthropic-version", "2023-06-01");
                post.setHeader("content-type", "application/json");
                post.setHeader("accept", "application/json");
                post.setHeader("accept-encoding", "gzip");
            });

    private static final Map<String, EngineBridge> ENGINE_BRIDGES = buildEngineBridges();

    private AbstractAIEngine() {
    }

    private static String internalTranslateUncached(String userMessage, String sourceLang,
                                                      String targetLanguageCode,
                                                      String engineName)
            throws IOException {
        TranslationRequest request = prepareTranslationRequest(userMessage, targetLanguageCode,
                engineName);
        String translation = executeApiTranslation(request, userMessage, sourceLang,
                targetLanguageCode, engineName, userMessage);
        ModLogger.debug("[TRANSLATION/API] [{}] {} => {}", targetLanguageCode, userMessage,
                translation);
        return translation;
    }

    public static String translateMessageWithSourceLangUncached(String text, String sourceLang,
                                                                String targetLanguage,
                                                                String engineName)
            throws IOException {
        return internalTranslateUncached(text, sourceLang, targetLanguage, engineName);
    }

    private static EngineBridge getEngineBridge(String engineName) {
        if (engineName == null) {
            throw new IllegalArgumentException("Unsupported engine: null");
        }

        EngineBridge bridge = ENGINE_BRIDGES.get(engineName.toLowerCase(Locale.ROOT));
        if (bridge == null) {
            throw new IllegalArgumentException("Unsupported engine: " + engineName);
        }
        return bridge;
    }

    private static TranslationRequest prepareTranslationRequest(String userMessage,
                                                               String targetLanguageCode,
                                                               String engineName) {
        EngineBridge bridge = getEngineBridge(engineName);

        ValidationService.ValidationResult textValidation =
                ValidationService.validateTranslationText(userMessage);
        if (!textValidation.isValid()) {
            throw new IllegalArgumentException(textValidation.getErrorMessage());
        }

        if (!ValidationService.isNotEmpty(targetLanguageCode)) {
            throw new IllegalArgumentException("Target language code cannot be empty");
        }

        ConfigurationManager configManager = ConfigurationManager.getInstance();
        String apiKey = configManager.getApiKey(engineName);
        bridge.validator.validateKey(apiKey);
        return new TranslationRequest(bridge, apiKey);
    }

    private static String executeApiTranslation(TranslationRequest request, String preparedMessage,
                                                String sourceLang, String targetLanguageCode,
                                                String engineName, String fallbackText)
            throws IOException {
        String payload = request.bridge.requestBuilder.build(preparedMessage, sourceLang,
                targetLanguageCode);
        String response = request.bridge.httpPoster.post(payload, request.apiKey);

        ModLogger.debug("[API_RESPONSE] [{}] Response received (length: {})", engineName,
                response.length());

        return request.bridge.responseParser.parse(response, fallbackText, targetLanguageCode);
    }

    private static Map<String, EngineBridge> buildEngineBridges() {
        Map<String, EngineBridge> bridges = new HashMap<>();

        bridges.put("openai", new EngineBridge(
                NetworkApiKeyTester.createOpenAI(),
                (text, sourceLang, targetLang) -> buildOpenAIResponsesBody(text, sourceLang,
                        targetLang),
                (payload, apiKey) -> BaseHttpClient.postJson(OPENAI_HTTP_CLIENT, payload, apiKey),
                (response, originalText, targetLang) ->
                        OpenAIResponseParser.getInstance()
                                .parseTranslation(response, originalText, targetLang)));

        bridges.put("claude", new EngineBridge(
                NetworkApiKeyTester.createClaude(),
                (text, sourceLang, targetLang) -> buildDefaultRequestBody("claude", text, sourceLang,
                        targetLang),
                (payload, apiKey) -> BaseHttpClient.postJson(CLAUDE_HTTP_CLIENT, payload, apiKey),
                (response, originalText, targetLang) ->
                        ClaudeResponseParser.getInstance()
                                .parseTranslation(response, originalText, targetLang)));

        return Collections.unmodifiableMap(bridges);
    }

    private static String buildDefaultRequestBody(String engineName, String text, String sourceLang,
                                                  String targetLang) {
        String model = ConfigurationManager.getInstance().getModelForEngine(engineName);
        return BaseAIRequestBuilder.buildClaudeRequestBody(text, sourceLang, targetLang, model,
                MAX_TOKENS);
    }

    private static String buildOpenAIResponsesBody(String text, String sourceLang,
                                                   String targetLang) {
        String model = ConfigurationManager.getInstance().getModelForEngine("openai");
        return BaseAIRequestBuilder.buildOpenAIResponsesRequestBody(text, sourceLang, targetLang,
                model, MAX_TOKENS, TEMPERATURE);
    }

}
