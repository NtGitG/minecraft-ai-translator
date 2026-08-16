package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpClient {

    private static final int HTTP_TIMEOUT_MS = 5000;
    private static final int MAX_HTTP_CONNECTIONS = 10;

    protected BaseHttpClient() {
    }

    protected abstract String getApiUrl();

    protected abstract void addHeaders(HttpPost post, String apiKey);

    protected static CloseableHttpClient getHttpClient() {
        return getSharedClient().getOrCreate();
    }

    public static String postJson(BaseHttpClient client, String payload, String apiKey)
            throws IOException {
        HttpPost post = new HttpPost(client.getApiUrl());
        post.setHeader("Content-Type", "application/json");
        client.addHeaders(post, apiKey);
        post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = getHttpClient().execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody =
                    entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";

            if (status != 200) {
                throw new ApiHttpException(status, responseBody);
            }

            return responseBody;
        }
    }

    public static void shutdownSharedClient() {
        getSharedClient().close();
    }

    private static SharedHttpClient getSharedClient() {
        return SingletonManager.getInstance(SharedHttpClient.class, SharedHttpClient::new);
    }

    private static CloseableHttpClient createHttpClient() throws Exception {
        try {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(MAX_HTTP_CONNECTIONS);
            cm.setDefaultMaxPerRoute(MAX_HTTP_CONNECTIONS);

            RequestConfig rc = RequestConfig.custom()
                    .setConnectTimeout(HTTP_TIMEOUT_MS)
                    .setSocketTimeout(HTTP_TIMEOUT_MS)
                    .setConnectionRequestTimeout(HTTP_TIMEOUT_MS)
                    .build();

            return HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(rc)
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                    .build();
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la creation du client HTTP", e);
            throw new Exception("Impossible de creer le client HTTP", e);
        }
    }

    private static final class SharedHttpClient {
        private final Object lock = new Object();
        private volatile CloseableHttpClient client;

        private CloseableHttpClient getOrCreate() {
            CloseableHttpClient current = client;
            if (current != null) {
                return current;
            }

            synchronized (lock) {
                if (client == null) {
                    try {
                        client = createHttpClient();
                        ModLogger.info("HTTP client initialise avec succes");
                    } catch (Exception e) {
                        ModLogger.error("Echec de l'initialisation du client HTTP", e);
                        throw new RuntimeException("Impossible d'initialiser le client HTTP", e);
                    }
                }
                return client;
            }
        }

        private void close() {
            synchronized (lock) {
                if (client == null) {
                    return;
                }

                try {
                    client.close();
                    ModLogger.info("HTTP client ferme");
                } catch (IOException e) {
                    ModLogger.warn("Erreur lors de la fermeture du client HTTP", e);
                } finally {
                    client = null;
                }
            }
        }
    }

    public static class ApiHttpException extends IOException {
        private final int statusCode;

        ApiHttpException(int statusCode, String body) {
            super("HTTP " + statusCode + " - " + body);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
