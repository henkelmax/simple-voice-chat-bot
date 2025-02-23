package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class LogsCommand {

    private static final String LOGS_COMMAND = "logs";

    public static void init() {
        CommandRegistry.registerCommand(LOGS_COMMAND, "Tells a user how to get logs",
                LogsCommand::onLogsCommand,
                Permission.MODERATE_MEMBERS
        );
    }

    private static void onLogsCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("How to get logs");
        builder.setDescription("""
                **Client Logs**
                On the client your logs are located in `.minecraft/logs/latest.log`.
                
                **Server Logs**
                On the server your logs are located in your Minecraft server directory in `logs/latest.log`.
                """);
        builder.setUrl("https://modrepo.de/minecraft/how_to_get_logs");

        event.getInteraction().replyEmbeds(builder.build()).queue();
    }

}
