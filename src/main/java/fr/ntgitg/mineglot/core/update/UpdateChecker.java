package fr.ntgitg.mineglot.core.update;

final class UpdateChecker {
    private final LatestReleaseSource releaseSource;
    private final String currentVersion;

    UpdateChecker(LatestReleaseSource releaseSource, String currentVersion) {
        if (releaseSource == null) {
            throw new IllegalArgumentException("releaseSource cannot be null");
        }
        this.releaseSource = releaseSource;
        this.currentVersion = currentVersion;
    }

    UpdateCheckResult check() {
        try {
            SemanticVersion installed = SemanticVersion.parse(currentVersion);
            ReleaseInfo latestRelease = releaseSource.fetchLatestRelease();
            if (latestRelease == null) {
                return UpdateCheckResult.upToDate();
            }

            SemanticVersion latest = SemanticVersion.parse(latestRelease.getTagName());
            if (latest.compareTo(installed) > 0) {
                return UpdateCheckResult.updateAvailable(latestRelease);
            }
            return UpdateCheckResult.upToDate();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = e.getClass().getSimpleName();
            }
            return UpdateCheckResult.unavailable(message);
        }
    }
}
