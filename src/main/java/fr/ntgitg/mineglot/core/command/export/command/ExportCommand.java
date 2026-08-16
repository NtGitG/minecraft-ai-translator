package fr.ntgitg.mineglot.core.command.export.command;

import fr.ntgitg.mineglot.core.command.base.AbstractCommand;
import fr.ntgitg.mineglot.core.command.export.handler.ExportHandler;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Collections;

public class ExportCommand extends AbstractCommand {

    public ExportCommand() {
        super("transexport", Collections.emptyList(), "Exporte vos statistiques personnelles d'usage",
                "[daily|weekly|monthly]");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ExportHandler.handleExportCommand(sender, args);
    }
}
