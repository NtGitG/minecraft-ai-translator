package fr.ntgitg.mineglot.core.command.base;

public final class CommandExecutionResult {
    private final boolean success;
    private final String message;
    private final int argsProcessed;

    private CommandExecutionResult(final boolean success, final String message,
                                   final int argsProcessed) {
        this.success = success;
        this.message = message;
        this.argsProcessed = argsProcessed;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getArgsProcessed() {
        return argsProcessed;
    }

    public static CommandExecutionResult success(final String message, final int argsProcessed) {
        return new CommandExecutionResult(true, message, argsProcessed);
    }

    public static CommandExecutionResult error(final String message) {
        return new CommandExecutionResult(false, message, 0);
    }
}
