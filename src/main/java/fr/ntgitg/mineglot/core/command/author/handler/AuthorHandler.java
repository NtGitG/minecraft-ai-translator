package fr.ntgitg.mineglot.core.command.author.handler;

import fr.ntgitg.mineglot.core.author.AuthorManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class AuthorHandler {

    public static void handleAuthorCommand(ICommandSender sender, String[] args)
            throws CommandException {
        ModLogger.debug("=== COMMANDE AUTHOR ===");
        ModLogger.debug("Affichage des informations de l'auteur (local)");

        try {
            AuthorManager.getInstance().displayAuthorInfo(sender, false);
            ModLogger.debug("Commande author exécutée avec succès");
        } catch (Exception e) {
            ModLogger.error("Erreur dans la commande author", e);
            throw new CommandException("Erreur lors de l'affichage des informations auteur");
        }
    }

    public static boolean isAuthorInfoAvailable() {
        return true;
    }
}
