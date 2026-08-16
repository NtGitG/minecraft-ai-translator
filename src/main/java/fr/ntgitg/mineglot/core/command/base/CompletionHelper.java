package fr.ntgitg.mineglot.core.command.base;

import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.utils.extractor.PlayerNameExtractor;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.ICommandSender;

import java.util.*;

public final class CompletionHelper {

    private CompletionHelper() {
        /* utility class */
    }

    public static List<String> getUniversalCompletions(String commandName, String[] args,
                                                       ICommandSender sender) {
        if (args == null || args.length == 0) {
            return Collections.emptyList();
        }

        int argIndex = args.length - 1;
        String currentArg = args[argIndex];

        try {
            switch (commandName.toLowerCase()) {
                case "translate":
                case "trs":
                    if (argIndex == 0) {
                        return getLiteralSuggestions(currentArg, "msg");
                    }
                    if (argIndex == 1 && "msg".equalsIgnoreCase(args[0])) {
                        return getPlayerNameSuggestions(sender, currentArg);
                    }
                    return Collections.emptyList();

                case "trs-clear":
                case "author":
                    return Collections.emptyList();

                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            ModLogger.error("Autocomplete error for " + commandName, e);
            return Collections.emptyList();
        }
    }

    private static List<String> getLiteralSuggestions(String partialValue, String... values) {
        String prefix = (partialValue != null ? partialValue : "").toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String value : values) {
            if (matchesPrefix(value, prefix)) {
                suggestions.add(value);
            }
        }
        return suggestions;
    }

    private static List<String> getPlayerNameSuggestions(ICommandSender sender,
                                                         String partialName) {
        Objects.requireNonNull(sender, "sender cannot be null");
        String prefix = (partialName != null ? partialName : "").toLowerCase(Locale.ROOT);

        Set<String> suggestions = new LinkedHashSet<>();
        try {
            List<String> onlinePlayers = PlayerNameManager.getInstance().getRawOnlinePlayerNames();
            for (String player : onlinePlayers) {
                if (PlayerNameExtractor.isBotName(player) || player.equals(sender.getName())) {
                    continue;
                }

                String baseName = PlayerNameExtractor.extractBaseName(player);
                if (matchesPrefix(player, prefix) || matchesPrefix(baseName, prefix)) {
                    suggestions.add(
                            PlayerNameExtractor.isValidPlayerName(baseName) ? baseName : player);
                }
            }
        } catch (Exception e) {
            ModLogger.error("Error while collecting player suggestions", e);
        }

        List<String> sortedSuggestions = new ArrayList<>(suggestions);
        Collections.sort(sortedSuggestions, String.CASE_INSENSITIVE_ORDER);
        return sortedSuggestions;
    }

    private static boolean matchesPrefix(String text, String prefix) {
        return text != null && text.toLowerCase(Locale.ROOT).startsWith(prefix);
    }
}
