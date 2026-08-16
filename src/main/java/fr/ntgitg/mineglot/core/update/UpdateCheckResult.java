package fr.ntgitg.mineglot.core.update;

public final class UpdateCheckResult {
    public enum Status {
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        UNAVAILABLE
    }

    private final Status status;
    private final ReleaseInfo releaseInfo;
    private final String failureReason;

    private UpdateCheckResult(Status status, ReleaseInfo releaseInfo, String failureReason) {
        this.status = status;
        this.releaseInfo = releaseInfo;
        this.failureReason = failureReason;
    }

    public static UpdateCheckResult updateAvailable(ReleaseInfo releaseInfo) {
        return new UpdateCheckResult(Status.UPDATE_AVAILABLE, releaseInfo, null);
    }

    public static UpdateCheckResult upToDate() {
        return new UpdateCheckResult(Status.UP_TO_DATE, null, null);
    }

    public static UpdateCheckResult unavailable(String failureReason) {
        return new UpdateCheckResult(Status.UNAVAILABLE, null, failureReason);
    }

    public Status getStatus() {
        return status;
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
