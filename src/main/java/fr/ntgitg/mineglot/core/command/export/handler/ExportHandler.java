package fr.ntgitg.mineglot.core.command.export.handler;

import fr.ntgitg.mineglot.core.command.export.services.StatsExporter;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ExportHandler {

    private static final String EXPORT_DIR = "config/mineglot/exports/";

    public static void handleExportCommand(ICommandSender sender, String[] args)
            throws CommandException {
        try {
            String period = resolvePeriod(sender, args);
            if (period == null) {
                return;
            }

            String reportJson = StatsExporter.generateReport(period);
            String filename = saveReportToFile(reportJson, period);
            sendExportSuccess(sender, filename);

            ModLogger.info("Rapport d'usage exporte: {}", filename);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de l'export des statistiques", e);
            ErrorManager.handleError(e, ErrorType.CONFIG, sender);
            throw new CommandException(I18nManager.getMessage("config.error.general"));
        }
    }

    private static String resolvePeriod(ICommandSender sender, String[] args) {
        String period = "monthly";
        if (args == null || args.length == 0) {
            return period;
        }

        String requested = args[0].toLowerCase(Locale.ROOT);
        if (isValidPeriod(requested)) {
            return requested;
        }

        sendInvalidPeriodMessage(sender);
        return null;
    }

    private static void sendInvalidPeriodMessage(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendError((EntityPlayer) sender, "export.invalid_period");
            return;
        }
        String message = I18nManager.getMessage("export.invalid_period");
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
    }

    private static void sendExportSuccess(ICommandSender sender, String filename) {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            MessageService.sendSuccess(player, "export.success", filename);
            MessageService.sendInfo(player, "export.location", EXPORT_DIR);
            return;
        }

        sender.addChatMessage(new ChatComponentText(I18nManager.getMessage("export.success", filename)));
        sender.addChatMessage(new ChatComponentText(I18nManager.getMessage("export.location", EXPORT_DIR)));
    }

    private static String saveReportToFile(String jsonContent, String period) throws IOException {
        File exportDir = new File(Minecraft.getMinecraft().mcDataDir, EXPORT_DIR);
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Impossible de creer le dossier d'export: " + exportDir.getAbsolutePath());
        }
        if (!exportDir.isDirectory()) {
            throw new IOException("Chemin d'export invalide: " + exportDir.getAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        String filename = "mineglot_stats_" + period + "_" + timestamp + ".json";
        File reportFile = new File(exportDir, filename);

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(jsonContent);
        }

        return filename;
    }

    private static boolean isValidPeriod(String period) {
        return "daily".equals(period) || "weekly".equals(period) || "monthly".equals(period);
    }
}
