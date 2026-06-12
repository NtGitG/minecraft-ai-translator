package fr.ntgitg.mineglot.core.service.error;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ModErrorEvent extends Event {
    private final ModException exception;
    private final String errorType;

    public ModErrorEvent(ModException exception, String errorType) {
        this.exception = exception;
        this.errorType = errorType;
    }

    public ModException getException() {
        return exception;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return exception.getMessage();
    }
}
