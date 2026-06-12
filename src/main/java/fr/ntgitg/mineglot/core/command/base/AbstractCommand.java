package fr.ntgitg.mineglot.core.command.base;

import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;

public abstract class AbstractCommand extends CommandBase {

    private final String commandName;
    private final List<String> commandAliases;
    private final String commandDescription;
    private final String commandUsageArgs;

    protected AbstractCommand(String name, List<String> aliases, String description,
                              String usageArgs) {
        this.commandName = name;
        this.commandAliases = aliases != null ? aliases : Collections.emptyList();
        this.commandDescription = description;
        this.commandUsageArgs = usageArgs != null ? usageArgs : "";
    }

    protected AbstractCommand(String name, String description, String usageArgs) {
        this(name, Collections.emptyList(), description, usageArgs);
    }

    protected AbstractCommand(String name, String description) {
        this(name, Collections.emptyList(), description, "");
    }

    @Override
    public final String getCommandName() {
        return commandName;
    }

    @Override
    public final List<String> getCommandAliases() {
        return commandAliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return String.format("§e/%s %s §7- %s", commandName, commandUsageArgs, commandDescription);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return CompletionHelper.getUniversalCompletions(getCommandName(), args, sender);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Toutes les commandes du mod sont accessibles à tous
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Toutes les commandes du mod sont utilisables par tous
    }

    protected final String getCommandDescription() {
        return commandDescription;
    }

    protected final String getCommandUsageArgs() {
        return commandUsageArgs;
    }

    protected String getCustomUsage() {
        return getCommandUsage(null);
    }

    protected void sendLocalizedMessage(ICommandSender sender, String i18nKey) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendInfo((EntityPlayer) sender, i18nKey);
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText(I18nManager.getMessage(i18nKey)));
        }
    }

    protected void sendLocalizedMessage(ICommandSender sender, String i18nKey, Object... args) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendInfo((EntityPlayer) sender, i18nKey, args);
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText(I18nManager.getMessage(i18nKey, args)));
        }
    }

    protected void sendTechnicalMessage(ICommandSender sender, String message) {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            if (player != null) {
                MessageService.sendInfo(player, "command.technical_message", message);
            }
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText(message));
        }
    }

    protected void sendLocalizedError(ICommandSender sender, String errorKey) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendError((EntityPlayer) sender, errorKey);
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText("§c" + I18nManager.getMessage(errorKey)));
        }
    }

    protected void sendLocalizedSuccess(ICommandSender sender, String successKey) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendSuccess((EntityPlayer) sender, successKey);
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText("§a" + I18nManager.getMessage(successKey)));
        }
    }

    protected void sendLocalizedSuccess(ICommandSender sender, String successKey, Object... args) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendSuccess((EntityPlayer) sender, successKey, args);
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText("§a" + I18nManager.getMessage(successKey, args)));
        }
    }

    protected void sendLocalizedError(ICommandSender sender, String errorKey, Object... args) {
        if (sender instanceof EntityPlayer) {
            MessageService.sendError((EntityPlayer) sender, errorKey, args);
        } else if (sender != null) {
            sender.addChatMessage(new ChatComponentText("§c" + I18nManager.getMessage(errorKey, args)));
        }
    }

    protected void sendLocalizedHelp(ICommandSender sender, String helpKey) {
        sendLocalizedMessage(sender, helpKey);
    }
}
