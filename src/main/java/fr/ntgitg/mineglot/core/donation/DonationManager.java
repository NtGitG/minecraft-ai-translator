package fr.ntgitg.mineglot.core.donation;

import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.awt.*;
import java.net.URI;

public class DonationManager {
    private static final String DONATION_URL = "https://buymeacoffee.com/ntgitg";

    public static void openDonationLink() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(DONATION_URL));
                    ModLogger.info("Lien de donation ouvert: {}", DONATION_URL);
                } else {
                    ModLogger.warn("Navigation web non supportée sur ce système");
                }
            } else {
                ModLogger.warn("Desktop non supporté sur ce système");
            }
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'ouverture du lien de donation", e);
            ErrorManager.handleError(e, ErrorType.UI, null);
        }
    }
}
