package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class CommandRegistry {

    private static final Map<String, Command> commands = new HashMap<>();

    public static void registerCommand(String command, String description, @Nullable List<OptionData> options, Consumer<SlashCommandInteractionEvent> listener, Permission... permissions) {
        if (commands.containsKey(command)) {
            throw new IllegalStateException("Command '%s' already registered".formatted(command));
        }

        SlashCommandData slash = Commands.slash(command, description);
        if (options != null) {
            slash.addOptions(options.toArray(new OptionData[0]));
        }
        slash.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissions));
        commands.put(command, new Command(command, listener, slash));
        Main.LOGGER.info("Added command '{}'", command);
    }

    public static void onCommand(SlashCommandInteractionEvent event) {
        Guild server = event.getInteraction().getGuild();
        if (server == null) {
            return;
        }
        if (server.getIdLong() != Environment.SERVER_ID) {
            return;
        }
        String cmdStr = event.getFullCommandName();
        Command command = commands.get(cmdStr);
        if (command == null) {
            Main.LOGGER.info("Could not find registered command '{}'", cmdStr);
            return;
        }
        command.listener.accept(event);
    }

    public static void registerCommand(String command, String description, @Nullable List<OptionData> options, Consumer<SlashCommandInteractionEvent> listener) {
        registerCommand(command, description, options, listener, Permission.MODERATE_MEMBERS);
    }

    public static void registerCommand(String command, String description, Consumer<SlashCommandInteractionEvent> listener, Permission... permissions) {
        registerCommand(command, description, null, listener, permissions);
    }

    public static void registerCommand(String command, String description, Consumer<SlashCommandInteractionEvent> listener) {
        registerCommand(command, description, null, listener, Permission.MODERATE_MEMBERS);
    }

    public static void applyCommands() {
        Main.API.updateCommands().addCommands(commands.values().stream().map(command -> command.slashCommand).toList()).queue();
        Main.LOGGER.info("Applied {} commands", commands.size());
    }

    private record Command(String command, Consumer<SlashCommandInteractionEvent> listener,
                           SlashCommandData slashCommand) {
    }

}
