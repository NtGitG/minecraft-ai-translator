package fr.ntgitg.mineglot.core.command.translate.translate_text.command;

import fr.ntgitg.mineglot.core.command.base.AbstractCommand;
import fr.ntgitg.mineglot.core.command.translate.translate_text.handler.TrsClearCommandHandler;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Collections;

public class TrsClearCommand extends AbstractCommand {

    public TrsClearCommand() {
        super("trs-clear", Collections.emptyList(), "Supprime la dernière traduction du cache", "");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return TrsClearCommandHandler.getFormattedUsage();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        TrsClearCommandHandler.handleTrsClearCommand(sender, args);
    }
}
