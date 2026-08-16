package fr.ntgitg.mineglot.core.service.error;

public class ModException extends RuntimeException {

    public ModException(String message) {
        super(message);
    }

    public ModException(String message, Throwable cause) {
        super(message, cause);
    }
}
