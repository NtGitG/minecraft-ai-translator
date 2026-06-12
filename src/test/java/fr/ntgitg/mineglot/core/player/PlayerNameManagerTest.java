package fr.ntgitg.mineglot.core.player;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlayerNameManagerTest {

    @Test
    public void visibleNamePrefersCleanDisplayNameOverInternalProfileName() {
        assertEquals("UXDZ",
                PlayerNameManager.resolveVisiblePlayerName("UXDZ", "|d8-02968442195c"));
    }

    @Test
    public void visibleNameFallsBackToValidProfileName() {
        assertEquals("Steve",
                PlayerNameManager.resolveVisiblePlayerName("", "Steve"));
    }

    @Test
    public void visibleNameRejectsInternalProfileNameWhenDisplayNameIsMissing() {
        assertEquals("",
                PlayerNameManager.resolveVisiblePlayerName("", "|d8-02968442195c"));
    }
}
