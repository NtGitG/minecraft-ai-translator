package fr.ntgitg.mineglot.core.command.author.command;

import fr.ntgitg.mineglot.core.command.author.handler.AuthorHandler;
import fr.ntgitg.mineglot.core.command.base.AbstractCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class AuthorCommand extends AbstractCommand {

    public AuthorCommand() {
        super("author", "Affiche les informations sur l'auteur du mod");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        AuthorHandler.handleAuthorCommand(sender, args);
    }
}
