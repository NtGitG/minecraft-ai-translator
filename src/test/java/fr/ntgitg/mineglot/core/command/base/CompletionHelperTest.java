package fr.ntgitg.mineglot.core.command.base;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompletionHelperTest {

    @Test
    public void translateFirstArgumentSuggestsExplicitMsgSubcommand() {
        assertEquals(Collections.singletonList("msg"),
                CompletionHelper.getUniversalCompletions(
                        "translate", new String[]{""}, null));
        assertEquals(Collections.singletonList("msg"),
                CompletionHelper.getUniversalCompletions(
                        "translate", new String[]{"M"}, null));
    }

    @Test
    public void translateDoesNotSuggestMsgAfterPublicTextHasStarted() {
        assertTrue(CompletionHelper.getUniversalCompletions(
                "translate", new String[]{"bonjour", ""}, null).isEmpty());
    }
}
