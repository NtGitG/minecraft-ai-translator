package fr.ntgitg.mineglot.core.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SemanticVersionTest {

    @Test
    public void acceptsGitHubVPrefix() {
        assertTrue(SemanticVersion.parse("v1.0.3")
                .compareTo(SemanticVersion.parse("1.0.2")) > 0);
    }

    @Test
    public void comparesNumericPartsNumerically() {
        assertTrue(SemanticVersion.parse("1.0.10")
                .compareTo(SemanticVersion.parse("1.0.9")) > 0);
    }

    @Test
    public void ignoresBuildMetadata() {
        assertEquals(0, SemanticVersion.parse("1.2.3+build.9")
                .compareTo(SemanticVersion.parse("1.2.3+build.10")));
    }

    @Test
    public void releaseIsNewerThanPreRelease() {
        assertTrue(SemanticVersion.parse("1.2.3")
                .compareTo(SemanticVersion.parse("1.2.3-rc.1")) > 0);
    }

    @Test
    public void comparesPreReleaseIdentifiersUsingSemVerRules() {
        assertTrue(SemanticVersion.parse("1.2.3-rc.2")
                .compareTo(SemanticVersion.parse("1.2.3-rc.1")) > 0);
        assertTrue(SemanticVersion.parse("1.2.3-beta")
                .compareTo(SemanticVersion.parse("1.2.3-2")) > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedVersion() {
        SemanticVersion.parse("release-three");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLeadingZeroInNumericPreReleaseIdentifier() {
        SemanticVersion.parse("1.2.3-01");
    }
}
