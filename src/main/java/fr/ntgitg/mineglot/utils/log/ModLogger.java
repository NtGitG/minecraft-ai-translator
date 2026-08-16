package fr.ntgitg.mineglot.utils.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModLogger {
    private static final Logger LOG = LogManager.getLogger("MineGlot");

    private ModLogger() {
    }

    public static void info(String fmt, Object... args) {
        LOG.info(fmt, args);
    }

    public static void warn(String fmt, Object... args) {
        LOG.warn(fmt, args);
    }

    public static void error(String fmt, Object... args) {
        LOG.error(fmt, args);
    }

    public static void error(String msg, Throwable t) {
        LOG.error(msg, t);
    }

    public static void debug(String fmt, Object... args) {
        LOG.debug(fmt, args);
    }
}
