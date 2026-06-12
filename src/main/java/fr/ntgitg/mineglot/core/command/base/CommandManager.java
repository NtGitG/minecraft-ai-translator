package fr.ntgitg.mineglot.core.command.base;

import fr.ntgitg.mineglot.core.command.author.command.AuthorCommand;
import fr.ntgitg.mineglot.core.command.export.command.ExportCommand;
import fr.ntgitg.mineglot.core.command.translate.translate_chat.command.TranslationCommand;
import fr.ntgitg.mineglot.core.command.translate.translate_text.command.TranslateCommand;
import fr.ntgitg.mineglot.core.command.translate.translate_text.command.TrsClearCommand;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CommandManager implements ICommandManager {
    private final Map<String, ICommand> commands = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    private static final List<Supplier<CommandBase>> REGISTERED_COMMANDS = Collections
            .unmodifiableList(Arrays.asList(TranslateCommand::new, TranslationCommand::new,
                    AuthorCommand::new, ExportCommand::new, TrsClearCommand::new));

    private CommandManager() {
    }

    public static CommandManager getInstance() {
        return SingletonManager.getInstance(CommandManager.class, CommandManager::new);
    }

    private boolean addCommand(final ICommand command) {
        String commandName = command.getCommandName().toLowerCase();

        if (commands.putIfAbsent(commandName, command) == null) {
            List<String> aliases = command.getCommandAliases();
            if (aliases != null) {
                for (String alias : aliases) {
                    commands.putIfAbsent(alias.toLowerCase(), command);
                }
            }
            return true;
        }
        return false;
    }

    public void initializeCommands() {
        if (initialized) {
            ModLogger.debug("Tentative de réinitialisation des commandes ignorée");
            return;
        }

        ModLogger.info("Initialisation des commandes...");

        int commandCount = 0;
        for (Supplier<CommandBase> commandSupplier : REGISTERED_COMMANDS) {
            ICommand command = null;
            try {
                command = commandSupplier.get();
                String commandName = command.getCommandName();

                if (addCommand(command)) {
                    ClientCommandHandler.instance.registerCommand(command);
                    commandCount++;
                } else {
                    ModLogger.warn("Commande dupliquée ignorée : {}", commandName);
                }
            } catch (Throwable t) {
                String commandName = command != null ? command.getCommandName() : "inconnue";
                ModLogger.error("Erreur lors de l'exécution de la commande {}", commandName, t);
            }
        }

        initialized = true;
        ModLogger.info("Commandes initialisées : {}", commandCount);
    }

    @Override
    public int executeCommand(ICommandSender sender, String rawCommand) {
        String[] args = rawCommand.split(" ");
        String commandName = args[0].toLowerCase();
        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        CommandExecutionResult result = executeCommand(commandName, sender, commandArgs);
        return result.isSuccess() ? result.getArgsProcessed() : 0;
    }

    @Override
    public List<String> getTabCompletionOptions(ICommandSender sender, String input, BlockPos pos) {
        if (input.isEmpty()) {
            return new ArrayList<>(commands.keySet());
        }

        String[] args = input.split(" ");
        String commandName = args[0].toLowerCase();
        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        return getTabCompletionOptions(commandName, sender, commandArgs);
    }

    @Override
    public List<ICommand> getPossibleCommands(ICommandSender sender) {
        return new ArrayList<>(commands.values());
    }

    @Override
    public Map<String, ICommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public ICommand getCommand(final String name) {
        return commands.get(name.toLowerCase());
    }

    public List<String> getCommandNames() {
        return new ArrayList<>(commands.keySet());
    }

    public boolean isCommandRegistered(final String name) {
        return commands.containsKey(name.toLowerCase());
    }

    public CommandExecutionResult executeCommand(final String commandName,
                                                 final ICommandSender sender, final String[] args) {
        final ICommand command = commands.get(commandName.toLowerCase());
        if (command == null) {
            ModLogger.warn("Commande inconnue : {}", commandName);
            return CommandExecutionResult
                    .error(I18nManager.getMessage("command.error.not_found", commandName));
        }

        try {
            command.processCommand(sender, args);
            return CommandExecutionResult.success(I18nManager.getMessage("command.success"),
                    args.length);
        } catch (CommandException e) {
            final String errorMessage = e.getMessage();
            if (sender instanceof EntityPlayer) {
                MessageService.sendError((EntityPlayer) sender, "command.error.general");
            } else {
                final ChatComponentText errorComponent =
                        new ChatComponentText(I18nManager.getMessage("command.error.general"));
                sender.addChatMessage(errorComponent);
            }
            ModLogger.error("Erreur lors de l'exécution de la commande {}", commandName, e);
            return CommandExecutionResult.error(errorMessage);
        }
    }

    public List<String> getTabCompletionOptions(final String commandName,
                                                final ICommandSender sender, final String[] args) {
        final ICommand command = commands.get(commandName.toLowerCase());
        if (command == null) {
            return new ArrayList<>();
        }
        BlockPos pos = sender.getPosition();
        if (pos == null) {
            pos = BlockPos.ORIGIN;
        }
        return command.addTabCompletionOptions(sender, args, pos);
    }
}
