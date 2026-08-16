package fr.ntgitg.mineglot.core.update;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GitHubLatestReleaseClientTest {

    @Test
    public void readsLatestPublishedRelease() throws Exception {
        RecordingTransport transport = new RecordingTransport(new ReleaseHttpResponse(200,
                "{\"tag_name\":\"v1.0.3\","
                        + "\"html_url\":\"https://github.com/NtGitG/"
                        + "minecraft-ai-translator/releases/tag/v1.0.3\","
                        + "\"draft\":false,\"prerelease\":false}"));

        ReleaseInfo release = new GitHubLatestReleaseClient(transport).fetchLatestRelease();

        assertEquals("v1.0.3", release.getTagName());
        assertEquals("1.0.3", release.getDisplayVersion());
        assertEquals(GitHubLatestReleaseClient.LATEST_RELEASE_URI, transport.requestUri);
        assertEquals("application/vnd.github+json", transport.headers.get("Accept"));
        assertTrue(transport.headers.get("User-Agent").contains("MineGlot"));
    }

    @Test(expected = IOException.class)
    public void treatsMissingReleaseAsUnavailable() throws Exception {
        RecordingTransport transport = new RecordingTransport(
                new ReleaseHttpResponse(404, "{\"message\":\"Not Found\"}"));

        new GitHubLatestReleaseClient(transport).fetchLatestRelease();
    }

    @Test(expected = IOException.class)
    public void rejectsApiErrors() throws Exception {
        RecordingTransport transport = new RecordingTransport(
                new ReleaseHttpResponse(429, "{\"message\":\"rate limited\"}"));

        new GitHubLatestReleaseClient(transport).fetchLatestRelease();
    }

    @Test(expected = IOException.class)
    public void rejectsMalformedJson() throws Exception {
        RecordingTransport transport = new RecordingTransport(
                new ReleaseHttpResponse(200, "not-json"));

        new GitHubLatestReleaseClient(transport).fetchLatestRelease();
    }

    @Test(expected = IOException.class)
    public void rejectsMissingFields() throws Exception {
        RecordingTransport transport = new RecordingTransport(
                new ReleaseHttpResponse(200, "{\"tag_name\":\"v1.0.3\"}"));

        new GitHubLatestReleaseClient(transport).fetchLatestRelease();
    }

    @Test(expected = IOException.class)
    public void rejectsReleaseLinksOutsideOfficialRepository() throws Exception {
        RecordingTransport transport = new RecordingTransport(new ReleaseHttpResponse(200,
                "{\"tag_name\":\"v1.0.3\","
                        + "\"html_url\":\"https://example.com/mineglot.jar\"}"));

        new GitHubLatestReleaseClient(transport).fetchLatestRelease();
    }

    private static final class RecordingTransport implements ReleaseHttpTransport {
        private final ReleaseHttpResponse response;
        private URI requestUri;
        private Map<String, String> headers;

        private RecordingTransport(ReleaseHttpResponse response) {
            this.response = response;
        }

        @Override
        public ReleaseHttpResponse get(URI uri, Map<String, String> requestHeaders) {
            this.requestUri = uri;
            this.headers = requestHeaders;
            return response;
        }
    }
}
