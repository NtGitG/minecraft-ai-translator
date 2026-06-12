package fr.ntgitg.mineglot.core.command.translate.translate_chat.command;

import fr.ntgitg.mineglot.core.command.base.AbstractCommand;
import fr.ntgitg.mineglot.core.command.translate.translate_chat.handler.TranslationCommandHandler;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Collections;

public class TranslationCommand extends AbstractCommand {

    public TranslationCommand() {
        super("translation", Collections.emptyList(),
                "Traduit le texte ou permet de sélectionner un message à traduire", "[texte]");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        TranslationCommandHandler.handleTranslationCommand(sender, args);
    }
}
