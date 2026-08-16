package fr.ntgitg.mineglot.core.update;

import fr.ntgitg.mineglot.utils.log.ModLogger;
import org.lwjgl.Sys;

import java.net.URI;

public final class ReleasePageOpener {
    private ReleasePageOpener() {
    }

    public static boolean open(URI releasePageUri) {
        if (!ReleaseUrlValidator.isTrusted(releasePageUri)) {
            ModLogger.warn("Ouverture refusee pour une URL de mise a jour non officielle");
            return false;
        }

        try {
            boolean opened = Sys.openURL(releasePageUri.toASCIIString());
            if (!opened) {
                ModLogger.warn("Le navigateur n'a pas pu ouvrir la page de mise a jour");
            }
            return opened;
        } catch (RuntimeException e) {
            ModLogger.warn("Erreur lors de l'ouverture de la page de mise a jour: {}",
                    e.getMessage());
            return false;
        }
    }
}
