package de.maxhenkel.voicechatbot;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Commands {

    public static final String CLOSE_COMMAND = "close";
    public static final String UNLOCK_COMMAND = "unlock";
    public static final String ISSUE_COMMAND = "issue";

    public static void clearCommands() {
        Main.LOGGER.info("Looking for server slash commands");
        Collection<Server> servers = Main.API.getServers();
        for (Server server : servers) {
            List<SlashCommand> commands = Main.API.getServerSlashCommands(server).join();
            for (SlashCommand command : commands) {
                command.deleteForServer(server).join();
                Main.LOGGER.info("Deleted command '{}' in server {}", command.getName(), server.getName());
            }
        }

        Main.LOGGER.info("Looking for global slash commands");
        List<SlashCommand> commands = Main.API.getGlobalSlashCommands().join();
        for (SlashCommand command : commands) {
            command.deleteGlobal().join();
            Main.LOGGER.info("Deleted command '{}'", command.getName());
        }
    }

    public static void initCommands() {
        SlashCommand.with(CLOSE_COMMAND, "Closes a support thread")
                .setDefaultEnabledForPermissions(PermissionType.MODERATE_MEMBERS)
                .createGlobal(Main.API).join();

        SlashCommand.with(UNLOCK_COMMAND, "Unlocks a thread")
                .setDefaultEnabledForPermissions(PermissionType.MODERATE_MEMBERS)
                .createGlobal(Main.API).join();

        SlashCommand.with(ISSUE_COMMAND, "Sends an issue template to a thread", Collections.singletonList(
                        SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "issue", "The issue to show", true,
                                SupportThread.ISSUES.stream().map(s -> SlashCommandOptionChoice.create(SupportThread.translateIssue(s), s)).collect(Collectors.toList()))
                ))
                .setDefaultEnabledForPermissions(PermissionType.MODERATE_MEMBERS)
                .createGlobal(Main.API).join();
    }

}
