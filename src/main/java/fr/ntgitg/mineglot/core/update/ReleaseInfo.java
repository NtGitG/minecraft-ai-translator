package fr.ntgitg.mineglot.core.update;

import java.net.URI;

public final class ReleaseInfo {
    private final String tagName;
    private final URI releasePageUri;

    public ReleaseInfo(String tagName, String releasePageUrl) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Release tag is empty");
        }
        this.tagName = tagName.trim();
        this.releasePageUri = ReleaseUrlValidator.requireTrusted(releasePageUrl);
    }

    public String getTagName() {
        return tagName;
    }

    public String getDisplayVersion() {
        if (tagName.length() > 1
                && (tagName.charAt(0) == 'v' || tagName.charAt(0) == 'V')) {
            return tagName.substring(1);
        }
        return tagName;
    }

    public URI getReleasePageUri() {
        return releasePageUri;
    }

    public String getReleasePageUrl() {
        return releasePageUri.toASCIIString();
    }
}
