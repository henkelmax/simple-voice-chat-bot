package de.maxhenkel.voicechatbot;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import javax.annotation.Nullable;
import java.util.*;

public class CommandRegistry {

    private static final Map<String, Command> commands = new HashMap<>();

    public static void init() {
        Main.LOGGER.info("Initializing command registry");

//        clearCommands();

        Main.API.addSlashCommandCreateListener(commandCreateEvent -> {
            String commandString = commandCreateEvent.getSlashCommandInteraction().getCommandName();
            Command command = commands.get(commandString);
            if (command == null) {
                Main.LOGGER.info("Could not find registered command '{}'", commandString);
                return;
            }
            command.listener.onSlashCommandCreate(commandCreateEvent);
        });
    }

    public static void registerCommand(String command, String description, @Nullable List<SlashCommandOption> options, SlashCommandCreateListener listener, PermissionType... permissions) {
        if (commands.containsKey(command)) {
            throw new IllegalStateException("Command '%s' already registered".formatted(command));
        }

        SlashCommandBuilder builder;
        if (options != null) {
            builder = SlashCommand.with(command, description, options);
        } else {
            builder = SlashCommand.with(command, description);
        }

//        builder.setDefaultEnabledForPermissions(permissions)
//                .createGlobal(Main.API)
//                .join();

        commands.put(command, new Command(command, listener));
        Main.LOGGER.info("Added command '{}'", command);
    }

    public static void registerCommand(String command, String description, @Nullable List<SlashCommandOption> options, SlashCommandCreateListener listener) {
        registerCommand(command, description, options, listener, PermissionType.MODERATE_MEMBERS);
    }

    public static void registerCommand(String command, String description, SlashCommandCreateListener listener, PermissionType... permissions) {
        registerCommand(command, description, null, listener, permissions);
    }

    public static void registerCommand(String command, String description, SlashCommandCreateListener listener) {
        registerCommand(command, description, null, listener, PermissionType.MODERATE_MEMBERS);
    }

    private static void clearCommands() {
        Main.LOGGER.info("Looking for server slash commands");
        Collection<Server> servers = Main.API.getServers();
        for (Server server : servers) {
            Set<SlashCommand> commands = Main.API.getServerSlashCommands(server).join();
            for (SlashCommand command : commands) {
                command.deleteForServer(server).join();
                Main.LOGGER.info("Deleted command '{}' in server {}", command.getName(), server.getName());
            }
        }

        Main.LOGGER.info("Looking for global slash commands");
        Set<SlashCommand> commands = Main.API.getGlobalSlashCommands().join();
        for (SlashCommand command : commands) {
            command.deleteGlobal().join();
            Main.LOGGER.info("Deleted command '{}'", command.getName());
        }
    }

    private record Command(String command, SlashCommandCreateListener listener) {
    }

}
