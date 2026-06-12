package fr.ntgitg.mineglot.core.chat;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChatTranslationFilterTest {

    @Test
    public void detectsPrivateMessageFormats() {
        assertTrue(ChatTranslationFilter.isPrivateMessage("From Steve: hello"));
        assertTrue(ChatTranslationFilter.isPrivateMessage("Alex tells you: hello"));
        assertTrue(ChatTranslationFilter.isPrivateMessage("[MP] Steve: salut"));
        assertTrue(ChatTranslationFilter.isPrivateMessage("PM from Alex: hello"));

        assertFalse(ChatTranslationFilter.isPrivateMessage("[Guild] Steve: hello"));
    }

    @Test
    public void acceptsPrivateMessageFormatsForTargetedPlayers() {
        assertTrue(ChatTranslationFilter.matchesTargetedChatShape("From Steve: hello",
                "Steve", "hello"));
        assertTrue(ChatTranslationFilter.matchesTargetedChatShape("Alex tells you: hello",
                "Alex", "hello"));
        assertTrue(ChatTranslationFilter.matchesTargetedChatShape("Alex whispers: hello",
                "Alex", "hello"));

        assertFalse(ChatTranslationFilter.matchesTargetedChatShape("Server notice: Steve joined",
                "Steve", "joined"));
    }

    @Test
    public void acceptsLikelyPlayerChatOnlyWhenPseudoIsFollowedByChatSeparator() {
        assertTrue(ChatTranslationFilter.isLikelyPlayerChat("[VIP] Steve: hello", "Steve",
                "hello"));
        assertTrue(ChatTranslationFilter.isLikelyPlayerChat("Steve \u00BB salut", "Steve",
                "salut"));
        assertTrue(ChatTranslationFilter.isLikelyPlayerChat("<Steve> hello", "Steve",
                "hello"));

        assertFalse(ChatTranslationFilter.isLikelyPlayerChat("Server notice: Steve joined",
                "Steve", "joined"));
        assertFalse(ChatTranslationFilter.isLikelyPlayerChat("[VIP] Steve hello", "Steve",
                "hello"));
        assertFalse(ChatTranslationFilter.isLikelyPlayerChat("Steve", "Steve", "hello"));
    }

    @Test
    public void acceptsMessagePositionWhenPseudoIsNotVisibleInRawText() {
        assertTrue(ChatTranslationFilter.isLikelyPlayerChat("[Rank] : bonjour", "Steve",
                "bonjour"));

        assertFalse(ChatTranslationFilter.isLikelyPlayerChat("[Rank] bonjour", "Steve",
                "bonjour"));
    }
}
