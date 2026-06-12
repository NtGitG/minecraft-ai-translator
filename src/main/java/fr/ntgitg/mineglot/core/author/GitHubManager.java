package fr.ntgitg.mineglot.core.author;

import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.awt.*;
import java.net.URI;

public class GitHubManager {
    private static final String GITHUB_URL = "https://github.com/ntgitg";

    public static void openGitHubProfile() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(GITHUB_URL));
                    ModLogger.info("Profil GitHub ouvert: {}", GITHUB_URL);
                } else {
                    ModLogger.warn("Navigation web non supportée sur ce système");
                }
            } else {
                ModLogger.warn("Desktop non supporté sur ce système");
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'ouverture du profil GitHub", e);
            ErrorManager.handleError(e, ErrorType.UI, null);
        }
    }
}
