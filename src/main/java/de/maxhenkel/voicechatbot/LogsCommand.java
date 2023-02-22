package de.maxhenkel.voicechatbot;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;

public class LogsCommand {

    private static final String LOGS_COMMAND = "logs";

    public static void init() {
        CommandRegistry.registerCommand(LOGS_COMMAND, "Tells a user how to get logs",
                LogsCommand::onLogsCommand,
                PermissionType.MODERATE_MEMBERS
        );
    }

    private static void onLogsCommand(SlashCommandCreateEvent event) {
        TextChannel channel = event.getSlashCommandInteraction().getChannel().orElse(null);
        if (channel == null) {
            return;
        }

        event.getSlashCommandInteraction().respondLater().thenAccept(responseUpdater -> {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("How to get logs");
            builder.setDescription("""
                    **Client Logs**
                    On the client your logs are located in `.minecraft/logs/latest.log`.
                                        
                    **Server Logs**
                    On the server your logs are located in your Minecraft server directory in `logs/latest.log`.
                    """);
            builder.setUrl("https://modrepo.de/minecraft/how_to_get_logs");
            channel.sendMessage(builder);
            responseUpdater.delete().exceptionally(new ExceptionHandler<>());

        }).exceptionally(new ExceptionHandler<>());
    }

}
