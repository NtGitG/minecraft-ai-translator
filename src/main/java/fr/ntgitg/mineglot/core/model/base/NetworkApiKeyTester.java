package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class NetworkApiKeyTester implements ApiKeyValidator {

    private static final int NETWORK_TIMEOUT_MS = 7000;
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(NETWORK_TIMEOUT_MS)
            .setConnectionRequestTimeout(NETWORK_TIMEOUT_MS)
            .setSocketTimeout(NETWORK_TIMEOUT_MS)
            .build();

    private final String engineName;
    private final Pattern keyPattern;
    private final String errorMessage;

    private NetworkApiKeyTester(String engineName, String pattern, String errorMessage) {
        this.engineName = engineName;
        this.keyPattern = Pattern.compile(pattern);
        this.errorMessage = errorMessage;
    }

    public static NetworkApiKeyTester createOpenAI() {
        return new NetworkApiKeyTester("OpenAI", "^sk-(proj-)?[a-zA-Z0-9\\-_]{20,}$",
                "Format de cle API invalide (doit commencer par sk- ou sk-proj-)");
    }

    public static NetworkApiKeyTester createClaude() {
        return new NetworkApiKeyTester("Claude", "^sk-ant-[a-zA-Z0-9\\-_]{20,}$",
                "Format de cle API invalide (doit commencer par sk-ant-)");
    }

    @Override
    public void validateKey(String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey");
        if (!keyPattern.matcher(apiKey).matches()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public void testApiKey(String apiKey) throws IOException {
        validateKey(apiKey);

        testApiKeyNetwork(engineName, apiKey);
    }

    public static void testApiKeyNetwork(String engine, String apiKey) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("Cle API vide pour " + engine);
        }

        try {
            switch (engine.toLowerCase()) {
                case "openai":
                    testOpenAIKey(apiKey);
                    break;
                case "claude":
                    testClaudeKey(apiKey);
                    break;
                default:
                    throw new IOException("Moteur non supporte: " + engine);
            }

            ModLogger.info("Test reseau reussi pour {}", engine);
        } catch (Exception e) {
            ModLogger.error("Test reseau echoue pour {} : {}", engine, e.getMessage());
            ModLogger.error("Type d'erreur : {} - Message : {}", e.getClass().getSimpleName(),
                    e.getMessage());
            throw new IOException("Erreur lors du test de la cle API " + engine + ": "
                    + e.getMessage(), e);
        }
    }

    private static void testOpenAIKey(String apiKey) throws IOException {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet(ProviderApiEndpoints.OPENAI_MODELS_URL);
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 401) {
                    throw new IOException("Cle API OpenAI invalide (401 Unauthorized)");
                } else if (statusCode != 200) {
                    throw new IOException("Erreur API OpenAI: " + statusCode);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                if (!responseBody.contains("\"data\"")) {
                    throw new IOException("Reponse API OpenAI invalide");
                }
            }
        }
    }

    private static void testClaudeKey(String apiKey) throws IOException {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet(ProviderApiEndpoints.CLAUDE_MODELS_URL);
            request.setHeader("x-api-key", apiKey);
            request.setHeader("anthropic-version", "2023-06-01");
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 401) {
                    throw new IOException("Cle API Claude invalide (401 Unauthorized)");
                } else if (statusCode != 200) {
                    throw new IOException("Erreur API Claude: " + statusCode);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                if (!responseBody.contains("\"data\"")) {
                    throw new IOException("Reponse API Claude invalide");
                }
            }
        }
    }

    public static void testCurrentEngineApiKey() throws IOException {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        String currentEngine = configManager.getCurrentEngine();
        String apiKey = configManager.getApiKey(currentEngine);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("Aucune cle API configuree pour " + currentEngine);
        }

        testApiKeyNetwork(currentEngine, apiKey);
    }

    public static boolean testAllConfiguredApiKeys() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        boolean allValid = true;

        for (String engine : new String[]{"openai", "claude"}) {
            String apiKey = configManager.getApiKey(engine);
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                try {
                    testApiKeyNetwork(engine, apiKey);
                    ModLogger.info("Cle API {} valide", engine);
                } catch (IOException e) {
                    ModLogger.error("Cle API {} invalide : {}", engine, e.getMessage());
                    allValid = false;
                }
            }
        }

        return allValid;
    }

    public String getEngineName() {
        return engineName;
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom().setDefaultRequestConfig(REQUEST_CONFIG).build();
    }
}
