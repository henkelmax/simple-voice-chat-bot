package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionCommand {

    private static final String VERSION_COMMAND = "version";

    public static void init() {
        CommandRegistry.registerCommand(VERSION_COMMAND, "Get the bot version",
                VersionCommand::onVersionCommand
        );
    }

    private static void onVersionCommand(SlashCommandInteractionEvent event) {
        event.getInteraction().reply("Bot version %s".formatted(getVersion())).setEphemeral(true).queue();
    }

    public static String getVersion() {
        try (InputStream inputStream = VersionCommand.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (inputStream == null) {
                return "N/A";
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty("version", "N/A");
        } catch (IOException e) {
            return "N/A";
        }
    }

}
