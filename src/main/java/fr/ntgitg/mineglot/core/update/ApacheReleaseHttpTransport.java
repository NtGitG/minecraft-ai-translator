package fr.ntgitg.mineglot.core.update;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class ApacheReleaseHttpTransport implements ReleaseHttpTransport {
    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT_MS)
            .setConnectionRequestTimeout(TIMEOUT_MS)
            .setSocketTimeout(TIMEOUT_MS)
            .build();

    @Override
    public ReleaseHttpResponse get(URI uri, Map<String, String> headers) throws IOException {
        HttpGet request = new HttpGet(uri);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.setHeader(header.getKey(), header.getValue());
        }

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .build();
             CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            String body = readBody(entity);
            return new ReleaseHttpResponse(response.getStatusLine().getStatusCode(), body);
        }
    }

    private static String readBody(HttpEntity entity) throws IOException {
        if (entity == null) {
            return "";
        }
        if (entity.getContentLength() > MAX_RESPONSE_BYTES) {
            throw new IOException("GitHub release response is too large");
        }

        try (InputStream input = entity.getContent();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > MAX_RESPONSE_BYTES) {
                    throw new IOException("GitHub release response is too large");
                }
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
