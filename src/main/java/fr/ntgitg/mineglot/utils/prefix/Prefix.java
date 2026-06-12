package fr.ntgitg.mineglot.utils.prefix;

public final class Prefix {

    private static final String RED = "§c";
    private static final String YELLOW = "§e";
    private static final String GREEN = "§a";
    private static final String WHITE = "§f";
    private static final String RESET = "§r";

    private static final String MOD_PREFIX =
            RED + "[" + RED + "§l" + "MineGlot" + RESET + RED + "] " + RESET + " ";

    private Prefix() {
    }

    public static String getMainPrefix() {
        return MOD_PREFIX;
    }

    public static String withMainPrefix(String message) {
        return MOD_PREFIX + message;
    }

    public static String error(String message) {
        return MOD_PREFIX + RED + message;
    }

    public static String success(String message) {
        return MOD_PREFIX + GREEN + message;
    }

    public static String warning(String message) {
        return MOD_PREFIX + YELLOW + message;
    }

    public static String info(String message) {
        return MOD_PREFIX + WHITE + message;
    }

    public static String translatedMessage(String playerName, String translatedMessage) {
        return MOD_PREFIX + WHITE + playerName + ": " + RESET + translatedMessage;
    }

    public static String translatedMessage(String translatedMessage) {
        return MOD_PREFIX + translatedMessage;
    }
}
