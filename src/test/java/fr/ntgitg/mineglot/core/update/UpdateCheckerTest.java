package fr.ntgitg.mineglot.core.update;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UpdateCheckerTest {

    @Test
    public void reportsNewerRelease() {
        UpdateChecker checker = new UpdateChecker(
                () -> release("v1.0.3"), "1.0.2");

        UpdateCheckResult result = checker.check();

        assertEquals(UpdateCheckResult.Status.UPDATE_AVAILABLE, result.getStatus());
        assertNotNull(result.getReleaseInfo());
        assertEquals("1.0.3", result.getReleaseInfo().getDisplayVersion());
    }

    @Test
    public void staysSilentForEqualRelease() {
        UpdateChecker checker = new UpdateChecker(
                () -> release("v1.0.2"), "1.0.2");

        UpdateCheckResult result = checker.check();

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, result.getStatus());
        assertNull(result.getReleaseInfo());
    }

    @Test
    public void staysSilentWhenInstalledVersionIsAhead() {
        UpdateChecker checker = new UpdateChecker(
                () -> release("v1.0.2"), "1.0.3");

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, checker.check().getStatus());
    }

    @Test
    public void treatsNoPublishedReleaseAsUpToDate() {
        UpdateChecker checker = new UpdateChecker(() -> null, "1.0.2");

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, checker.check().getStatus());
    }

    @Test
    public void convertsNetworkFailuresToUnavailableResult() {
        UpdateChecker checker = new UpdateChecker(() -> {
            throw new IOException("offline");
        }, "1.0.2");

        UpdateCheckResult result = checker.check();

        assertEquals(UpdateCheckResult.Status.UNAVAILABLE, result.getStatus());
        assertEquals("offline", result.getFailureReason());
    }

    @Test
    public void rejectsInvalidRemoteTagsWithoutShowingAnUpdate() {
        UpdateChecker checker = new UpdateChecker(
                () -> release("latest"), "1.0.2");

        assertEquals(UpdateCheckResult.Status.UNAVAILABLE, checker.check().getStatus());
    }

    private static ReleaseInfo release(String tagName) {
        return new ReleaseInfo(tagName,
                "https://github.com/NtGitG/minecraft-ai-translator/releases/tag/" + tagName);
    }
}
