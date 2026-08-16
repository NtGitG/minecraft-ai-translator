package fr.ntgitg.mineglot.core.chat;

import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.core.player.PlayerNameManager.OnlinePlayerNameSnapshot;
import fr.ntgitg.mineglot.utils.detector.PlayerBotDetector;
import fr.ntgitg.mineglot.utils.extractor.PlayerNameExtractor;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessageParser {

    private static final Pattern VANILLA_ANGLE_FORMAT = Pattern.compile("<([^>]+)>");
    private static final Pattern[] PRIVATE_MESSAGE_FORMATS = {
            Pattern.compile("^From\\s+([A-Za-z0-9_]{1,16})\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^PM\\s+from\\s+([A-Za-z0-9_]{1,16})\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\[MP\\]\\s*([A-Za-z0-9_]{1,16})\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^([A-Za-z0-9_]{1,16})\\s+(?:tells you|whispers)\\s*:\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE)
    };
    private static final Pattern DIRECT_MESSAGE_COMMAND =
            Pattern.compile("^/(?:msg|tell|w|whisper|pm|dm)\\s+([A-Za-z0-9_]{1,16})\\b");
    private static final Pattern NON_PLAYER_NAME_CHARS = Pattern.compile("[^a-zA-Z0-9_]");
    private static final Pattern PLAYER_TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9_]+");
    private static final Pattern WHITESPACE_SPLIT = Pattern.compile("\\s+");

    public static ParsedChat extract(ClientChatReceivedEvent event) {
        IChatComponent msg = event.message;
        String raw = msg.getUnformattedText();
        OnlinePlayerNameSnapshot players =
                PlayerNameManager.getInstance().getOnlinePlayerNameSnapshot();

        ParsedChat rawParsed = extractFromTextInternal(raw, players.getRawNameSet());
        if (rawParsed != null) {
            return rawParsed;
        }

        // Analyse des composants du chat quand le texte brut ne suffit pas.
        List<IChatComponent> parts = collectComponents(msg);
        for (int i = 0; i < parts.size(); i++) {
            String pseudo = extractPseudoFromComponent(parts.get(i), players.getRawNameSet());
            if (pseudo == null) {
                continue;
            }

            String message = extractMessageFromParts(parts, i);
            if (message == null || message.isEmpty()) {
                message = extractMessageFromRaw(msg.getUnformattedText(), pseudo);
            }

            if (message != null && !message.isEmpty()) {
                return new ParsedChat(pseudo, message);
            }
        }

        return null;
    }

    public static ParsedChat extractFromText(String raw, Set<String> onlinePlayers) {
        Set<String> players = onlinePlayers != null ? onlinePlayers : Collections.emptySet();
        return extractFromTextInternal(raw, players);
    }

    private static ParsedChat extractFromTextInternal(String raw, Set<String> players) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        ParsedChat vanilla = extractVanillaAngleFormat(raw, players);
        if (vanilla != null) {
            return vanilla;
        }

        ParsedChat privateMessage = extractPrivateMessageFormat(raw, players);
        if (privateMessage != null) {
            return privateMessage;
        }

        return extractKnownPlayerFormat(raw, players);
    }

    private static ParsedChat extractVanillaAngleFormat(String raw, Set<String> players) {
        if (raw.indexOf('<') < 0 || raw.indexOf('>') < 0) {
            return null;
        }

        Matcher matcher = VANILLA_ANGLE_FORMAT.matcher(raw);
        while (matcher.find()) {
            String possiblePseudo = matcher.group(1).trim();
            String resolved = resolvePseudoCandidate(possiblePseudo, players, false);
            if (resolved == null && !PlayerBotDetector.isBotName(possiblePseudo)) {
                resolved = possiblePseudo;
            }

            String message = stripLeadingSeparators(raw.substring(matcher.end()));
            if (resolved != null && !message.isEmpty()) {
                return new ParsedChat(resolved, message);
            }
        }
        return null;
    }

    private static ParsedChat extractPrivateMessageFormat(String raw, Set<String> players) {
        if (!mightBePrivateMessage(raw)) {
            return null;
        }

        for (Pattern pattern : PRIVATE_MESSAGE_FORMATS) {
            Matcher matcher = pattern.matcher(raw);
            if (!matcher.find()) {
                continue;
            }

            String pseudo = resolvePseudoCandidate(matcher.group(1), players, false);
            String message = matcher.group(2).trim();
            if (pseudo != null && !message.isEmpty()) {
                return new ParsedChat(pseudo, message);
            }
        }

        return null;
    }

    /**
     * Scanne le message une seule fois en O(message) : on isole chaque token de
     * caractères de pseudo (les bornes sont garanties par construction) puis on teste
     * l'appartenance au set des joueurs en O(1). Remplace l'ancien parcours
     * O(joueurs x message) qui faisait un indexOf pour chaque joueur connecté.
     */
    private static ParsedChat extractKnownPlayerFormat(String raw, Set<String> players) {
        if (players == null || players.isEmpty()) {
            return null;
        }

        int length = raw.length();
        int index = 0;
        while (index < length) {
            if (!isPlayerNameChar(raw.charAt(index))) {
                index++;
                continue;
            }

            int start = index;
            while (index < length && isPlayerNameChar(raw.charAt(index))) {
                index++;
            }

            String token = raw.substring(start, index);
            if (players.contains(token) && !PlayerBotDetector.isBotName(token)) {
                String message = extractMessageAfterPseudo(raw, index);
                if (message != null && !message.isEmpty()) {
                    return new ParsedChat(token, message);
                }
            }
        }

        return null;
    }

    private static String extractPseudoFromComponent(IChatComponent component, Set<String> players) {
        if (component == null) {
            return null;
        }

        String txt = component.getUnformattedText();
        String resolved = resolvePseudoCandidate(txt, players);
        if (resolved != null) {
            return resolved;
        }

        if (component.getChatStyle() != null) {
            String insertion = component.getChatStyle().getInsertion();
            if (insertion != null && !insertion.isEmpty()) {
                String cleaned = PlayerNameExtractor.cleanName(insertion);
                String fromInsertion = resolvePseudoCandidate(cleaned, players);
                if (fromInsertion != null) {
                    return fromInsertion;
                }
            }

            ClickEvent clickEvent = component.getChatStyle().getChatClickEvent();
            String fromClick = extractPseudoFromClickEvent(clickEvent, players);
            if (fromClick != null) {
                return fromClick;
            }

            HoverEvent hoverEvent = component.getChatStyle().getChatHoverEvent();
            String fromHover = extractPseudoFromHoverEvent(hoverEvent, players);
            if (fromHover != null) {
                return fromHover;
            }
        }

        return null;
    }

    private static String extractPseudoFromClickEvent(ClickEvent clickEvent, Set<String> players) {
        if (clickEvent == null || clickEvent.getValue() == null) {
            return null;
        }

        String value = clickEvent.getValue().trim();
        if (value.isEmpty()) {
            return null;
        }

        if (clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND
                || clickEvent.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
            String fromCommand = extractPseudoFromCommand(value, players);
            if (fromCommand != null) {
                return fromCommand;
            }
        }

        String resolved = resolvePseudoCandidate(value, players);
        if (resolved != null) {
            return resolved;
        }

        String[] parts = WHITESPACE_SPLIT.split(value);
        for (String part : parts) {
            String candidate = NON_PLAYER_NAME_CHARS.matcher(part).replaceAll("");
            resolved = resolvePseudoCandidate(candidate, players);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    private static String extractPseudoFromHoverEvent(HoverEvent hoverEvent, Set<String> players) {
        if (hoverEvent == null || hoverEvent.getValue() == null) {
            return null;
        }

        String hoverText = hoverEvent.getValue().getUnformattedText();
        if (hoverText == null || hoverText.isEmpty()) {
            return null;
        }

        String fromCommand = extractPseudoFromCommand(hoverText, players);
        if (fromCommand != null) {
            return fromCommand;
        }

        String[] tokens = PLAYER_TOKEN_SPLIT.split(hoverText);
        for (String token : tokens) {
            String resolved = resolvePseudoCandidate(token, players);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static String extractMessageFromParts(List<IChatComponent> parts, int nameIndex) {
        if (parts == null || nameIndex < 0 || nameIndex >= parts.size() - 1) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (int j = nameIndex + 1; j < parts.size(); j++) {
            String txt = parts.get(j).getUnformattedText();
            if (txt == null || txt.isEmpty()) {
                continue;
            }

            if (!started) {
                String trimmed = txt.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (isSeparatorOnly(trimmed)) {
                    continue;
                }
                started = true;
            }
            sb.append(txt);
        }

        String message = sb.toString().trim();
        message = stripLeadingSeparators(message);
        return message.isEmpty() ? null : message;
    }

    private static boolean isSeparatorOnly(String text) {
        return ":".equals(text) || ">".equals(text) || "\u00BB".equals(text);
    }

    private static String extractMessageFromRaw(String raw, String pseudo) {
        if (raw == null || pseudo == null) {
            return null;
        }

        int idx = raw.indexOf(pseudo);
        if (idx < 0) {
            return null;
        }
        return extractMessageAfterPseudo(raw, idx + pseudo.length());
    }

    private static String extractMessageAfterPseudo(String raw, int pseudoEndIndex) {
        if (raw == null || pseudoEndIndex < 0 || pseudoEndIndex > raw.length()) {
            return null;
        }
        String after = raw.substring(pseudoEndIndex).trim();
        if (!startsWithSeparator(after)) {
            return null;
        }
        after = stripLeadingSeparators(after);
        return after.isEmpty() ? null : after;
    }

    private static boolean startsWithSeparator(String text) {
        if (text == null) {
            return false;
        }
        String result = text.trim();
        return result.startsWith("->") || result.startsWith(":") || result.startsWith(">")
                || result.startsWith("\u00BB");
    }

    private static String stripLeadingSeparators(String text) {
        if (text == null) {
            return null;
        }
        String result = text.trim();
        while (result.startsWith("->") || result.startsWith(":") || result.startsWith(">")
                || result.startsWith("\u00BB")) {
            if (result.startsWith("->")) {
                result = result.substring(2).trim();
                continue;
            }
            result = result.substring(1).trim();
        }
        return result;
    }

    private static List<IChatComponent> collectComponents(IChatComponent root) {
        List<IChatComponent> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        Map<IChatComponent, Boolean> seen = new IdentityHashMap<>();
        Deque<IChatComponent> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            IChatComponent current = stack.pop();
            if (current == null || seen.containsKey(current)) {
                continue;
            }
            seen.put(current, Boolean.TRUE);
            result.add(current);

            List<IChatComponent> siblings = current.getSiblings();
            if (siblings != null && !siblings.isEmpty()) {
                for (int i = siblings.size() - 1; i >= 0; i--) {
                    stack.push(siblings.get(i));
                }
            }
        }

        return result;
    }

    private static String extractPseudoFromCommand(String value, Set<String> players) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Matcher matcher = DIRECT_MESSAGE_COMMAND.matcher(trimmed);
        if (matcher.find()) {
            String candidate = matcher.group(1);
            String resolved = resolvePseudoCandidate(candidate, players);
            if (resolved != null) {
                return resolved;
            }
        }

        String[] parts = WHITESPACE_SPLIT.split(trimmed);
        if (parts.length >= 2 && parts[0].startsWith("/")) {
            String candidate = PlayerNameExtractor.cleanName(parts[1]);
            String resolved = resolvePseudoCandidate(candidate, players);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    private static boolean mightBePrivateMessage(String raw) {
        return startsWithIgnoreCase(raw, "From")
                || startsWithIgnoreCase(raw, "PM")
                || startsWithIgnoreCase(raw, "[MP]")
                || containsIgnoreCase(raw, "tells you")
                || containsIgnoreCase(raw, "whispers");
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.length() >= prefix.length()
                && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        int max = value.length() - needle.length();
        for (int i = 0; i <= max; i++) {
            if (value.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPlayerNameChar(char value) {
        return value == '_'
                || (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z')
                || (value >= '0' && value <= '9');
    }

    private static String resolvePseudoCandidate(String candidate, Set<String> players) {
        return resolvePseudoCandidate(candidate, players, true);
    }

    private static String resolvePseudoCandidate(String candidate, Set<String> players,
                                                 boolean allowPlayerNameManager) {
        if (candidate == null) {
            return null;
        }

        String normalized = candidate.trim();
        if (normalized.isEmpty() || PlayerBotDetector.isBotName(normalized)) {
            return null;
        }

        if (players.contains(normalized)) {
            return normalized;
        }

        if (!allowPlayerNameManager) {
            return null;
        }

        String realName = PlayerNameManager.getInstance().getRealPlayerName(normalized);
        if (realName != null) {
            String normalizedRealName = realName.trim();
            if (!normalizedRealName.isEmpty()
                    && !PlayerBotDetector.isBotName(normalizedRealName)) {
                return normalizedRealName;
            }
        }

        return null;
    }

    public static class ParsedChat {
        public final String pseudo;
        public final String message;

        public ParsedChat(String pseudo, String message) {
            this.pseudo = pseudo;
            this.message = message;
        }
    }
}
