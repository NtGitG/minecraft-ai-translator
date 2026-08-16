package fr.ntgitg.mineglot.core.update;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class ReleaseUrlValidator {
    private static final String TRUSTED_HOST = "github.com";
    private static final String TRUSTED_PATH_PREFIX =
            "/ntgitg/minecraft-ai-translator/releases/";

    private ReleaseUrlValidator() {
    }

    public static URI requireTrusted(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Release URL is empty");
        }

        final URI uri;
        try {
            uri = new URI(url.trim()).normalize();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Release URL is invalid", e);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        int port = uri.getPort();

        boolean trusted = "https".equalsIgnoreCase(scheme)
                && TRUSTED_HOST.equalsIgnoreCase(host)
                && (port == -1 || port == 443)
                && uri.getUserInfo() == null
                && path != null
                && path.toLowerCase(Locale.ROOT).startsWith(TRUSTED_PATH_PREFIX);

        if (!trusted) {
            throw new IllegalArgumentException("Release URL is not an official MineGlot URL");
        }

        return uri;
    }

    public static boolean isTrusted(URI uri) {
        if (uri == null) {
            return false;
        }
        try {
            requireTrusted(uri.toASCIIString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
