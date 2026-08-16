package fr.ntgitg.mineglot.core.translation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TranslationCacheApiFlowTest {

    @Test
    public void targetedChatDoesNotUpdateTrsClearState() {
        assertFalse(TranslationCacheApiFlow.canUseTrsClear(true));
        assertTrue(TranslationCacheApiFlow.canUseTrsClear(false));
    }

    @Test
    public void targetedChatMissDoesNotShowLoadingMessage() {
        assertFalse(TranslationCacheApiFlow.shouldShowLoadingMessage(true, false, null));
    }

    @Test
    public void manualAndPrivateTranslationsStillShowLoadingMessage() {
        assertTrue(TranslationCacheApiFlow.shouldShowLoadingMessage(false, false, null));
        assertTrue(TranslationCacheApiFlow.shouldShowLoadingMessage(true, true, null));
        assertTrue(TranslationCacheApiFlow.shouldShowLoadingMessage(true, false, "Steve"));
    }
}
