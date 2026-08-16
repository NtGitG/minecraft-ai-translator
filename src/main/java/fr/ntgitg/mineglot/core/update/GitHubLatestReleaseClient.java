package fr.ntgitg.mineglot.core.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class GitHubLatestReleaseClient implements LatestReleaseSource {
    static final URI LATEST_RELEASE_URI = URI.create(
            "https://api.github.com/repos/NtGitG/minecraft-ai-translator/releases/latest");

    private static final Map<String, String> REQUEST_HEADERS;

    static {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("User-Agent", "MineGlot-UpdateChecker");
        REQUEST_HEADERS = Collections.unmodifiableMap(headers);
    }

    private final ReleaseHttpTransport transport;

    GitHubLatestReleaseClient() {
        this(new ApacheReleaseHttpTransport());
    }

    GitHubLatestReleaseClient(ReleaseHttpTransport transport) {
        if (transport == null) {
            throw new IllegalArgumentException("transport cannot be null");
        }
        this.transport = transport;
    }

    @Override
    public ReleaseInfo fetchLatestRelease() throws IOException {
        ReleaseHttpResponse response = transport.get(LATEST_RELEASE_URI, REQUEST_HEADERS);
        int statusCode = response.getStatusCode();

        if (statusCode != 200) {
            throw new IOException("GitHub releases API returned HTTP " + statusCode);
        }

        return parseRelease(response.getBody());
    }

    private ReleaseInfo parseRelease(String responseBody) throws IOException {
        try {
            JsonElement parsed = new JsonParser().parse(responseBody);
            if (!parsed.isJsonObject()) {
                throw new IOException("GitHub release response is not a JSON object");
            }

            JsonObject release = parsed.getAsJsonObject();
            if (getOptionalBoolean(release, "draft")
                    || getOptionalBoolean(release, "prerelease")) {
                return null;
            }

            String tagName = getRequiredString(release, "tag_name");
            String releasePageUrl = getRequiredString(release, "html_url");
            return new ReleaseInfo(tagName, releasePageUrl);
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException e) {
            throw new IOException("Invalid GitHub release response", e);
        }
    }

    private static boolean getOptionalBoolean(JsonObject object, String memberName) {
        JsonElement value = object.get(memberName);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private static String getRequiredString(JsonObject object, String memberName)
            throws IOException {
        JsonElement value = object.get(memberName);
        if (value == null || value.isJsonNull()) {
            throw new IOException("Missing GitHub release field: " + memberName);
        }

        String text = value.getAsString();
        if (text == null || text.trim().isEmpty()) {
            throw new IOException("Empty GitHub release field: " + memberName);
        }
        return text;
    }
}
