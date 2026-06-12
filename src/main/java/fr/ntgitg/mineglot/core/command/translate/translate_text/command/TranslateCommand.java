package fr.ntgitg.mineglot.core.command.translate.translate_text.command;

import fr.ntgitg.mineglot.core.command.base.AbstractCommand;
import fr.ntgitg.mineglot.core.command.translate.translate_text.handler.TranslateCommandHandler;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;

public class TranslateCommand extends AbstractCommand {

    public TranslateCommand() {
        super("translate", Arrays.asList("trs"), "Traduit le texte fourni", "<texte>");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return TranslateCommandHandler.getFormattedUsage();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        TranslateCommandHandler.handleTranslateCommand(sender, args);
    }
}
