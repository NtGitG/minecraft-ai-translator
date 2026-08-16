package fr.ntgitg.mineglot.core.update;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertTrue;

public class ReleaseUrlValidatorTest {

    @Test
    public void acceptsOfficialReleasePage() {
        URI uri = ReleaseUrlValidator.requireTrusted(
                "https://github.com/NtGitG/minecraft-ai-translator/releases/tag/v1.0.3");

        assertTrue(ReleaseUrlValidator.isTrusted(uri));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsHttpLinks() {
        ReleaseUrlValidator.requireTrusted(
                "http://github.com/NtGitG/minecraft-ai-translator/releases/tag/v1.0.3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLookalikeHosts() {
        ReleaseUrlValidator.requireTrusted(
                "https://github.com.example.org/NtGitG/minecraft-ai-translator/releases/tag/v1.0.3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOtherGitHubRepositories() {
        ReleaseUrlValidator.requireTrusted(
                "https://github.com/other/project/releases/tag/v1.0.3");
    }
}
