package fr.ntgitg.mineglot.core.chat;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ChatMessageParserTest {

    @Test
    public void extractsVanillaAngleMessageWithoutOnlinePlayers() {
        assertParsed("<Steve> hello everyone", emptyPlayers(), "Steve", "hello everyone");
    }

    @Test
    public void extractsVanillaAngleMessageWithColonSeparator() {
        assertParsed("<Steve>: gg", emptyPlayers(), "Steve", "gg");
    }

    @Test
    public void extractsRankedColonMessage() {
        assertParsed("[VIP] Steve: hello everyone", players(), "Steve", "hello everyone");
    }

    @Test
    public void extractsGuildRankedColonMessage() {
        assertParsed("[Guild] [MVP+] Alex: bonjour", players(), "Alex", "bonjour");
    }

    @Test
    public void extractsGuillemetMessage() {
        assertParsed("Steve \u00BB salut tout le monde", players(), "Steve",
                "salut tout le monde");
    }

    @Test
    public void extractsPrivateFromMessage() {
        assertParsed("From Steve: salut", players(), "Steve", "salut");
    }

    @Test
    public void extractsPrivateFromMessageWithFlexibleSpacing() {
        assertParsed("From   Steve : salut", players(), "Steve", "salut");
    }

    @Test
    public void extractsWhisperMessage() {
        assertParsed("Alex whispers: meet at spawn", players(), "Alex", "meet at spawn");
    }

    @Test
    public void extractsWhisperMessageWithSpaceBeforeSeparator() {
        assertParsed("Alex whispers : meet at spawn", players(), "Alex", "meet at spawn");
    }

    @Test
    public void ignoresDecoratedMessageFromUnknownPlayer() {
        ChatMessageParser.ParsedChat parsed =
                ChatMessageParser.extractFromText("[VIP] Unknown: hello", players());

        assertNull(parsed);
    }

    @Test
    public void ignoresKnownPlayerMentionWithoutChatSeparator() {
        ChatMessageParser.ParsedChat parsed =
                ChatMessageParser.extractFromText("Server notice: Steve joined the lobby", players());

        assertNull(parsed);
    }

    @Test
    public void ignoresKnownPlayerEmbeddedInLongerToken() {
        ChatMessageParser.ParsedChat parsed =
                ChatMessageParser.extractFromText("NotSteve: hello", players("Steve"));

        assertNull(parsed);
    }

    @Test
    public void ignoresBotNameEvenWhenListedAsOnlinePlayer() {
        ChatMessageParser.ParsedChat parsed =
                ChatMessageParser.extractFromText("|1f-aaa0faf55fd1: server notice",
                        players("|1f-aaa0faf55fd1"));

        assertNull(parsed);
    }

    @Test
    public void ignoresBotNameInVanillaAngleFormat() {
        ChatMessageParser.ParsedChat parsed =
                ChatMessageParser.extractFromText("<|1f-aaa0faf55fd1>: server notice",
                        players("|1f-aaa0faf55fd1"));

        assertNull(parsed);
    }

    @Test
    public void keepsValidPlayerParsingWhenBotNamesArePresent() {
        assertParsed("Steve: hello", players("Steve", "|1f-aaa0faf55fd1"), "Steve",
                "hello");
    }

    @Test
    public void usesPlayerOccurrenceFollowedByChatSeparator() {
        assertParsed("[Mention Steve] [VIP] Steve: real message", players(), "Steve",
                "real message");
    }

    private static void assertParsed(String raw, Set<String> onlinePlayers, String expectedPseudo,
                                     String expectedMessage) {
        ChatMessageParser.ParsedChat parsed =
                ChatMessageParser.extractFromText(raw, onlinePlayers);

        assertNotNull(parsed);
        assertEquals(expectedPseudo, parsed.pseudo);
        assertEquals(expectedMessage, parsed.message);
    }

    private static Set<String> players(String... names) {
        if (names.length == 0) {
            names = new String[]{"Steve", "Alex", "Fresh_123"};
        }
        return new HashSet<>(Arrays.asList(names));
    }

    private static Set<String> emptyPlayers() {
        return new HashSet<>();
    }
}
