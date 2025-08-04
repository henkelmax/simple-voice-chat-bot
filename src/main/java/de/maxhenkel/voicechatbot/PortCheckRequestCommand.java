package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class PortCheckRequestCommand {

    private static final String PORT_CHECK_REQUEST_COMMAND = "portcheckrequest";

    public static void init() {
        CommandRegistry.registerCommand(PORT_CHECK_REQUEST_COMMAND, "Asks a user their server IP and voice chat port for the port checker",
                PortCheckRequestCommand::onPortCheckRequest,
                Permission.MODERATE_MEMBERS
        );
    }

    private static void onPortCheckRequest(SlashCommandInteractionEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Port Checking");
        builder.setDescription("""
                To verify that your voice chat port is open, please send us the following details:
                
                - **Without a proxy**: Your __server's public IP address__ and __voice chat port__.
                - **With a proxy**: Your __proxy's public IP address__ and __the proxy's voice chat port__.
                
                Make sure your server or proxy is running when you share this information. We'll then ping the specified port to confirm it's open.
                No player whitelisting or special setup required for this.
                """);
        builder.setColor(Color.BLUE);
        builder.setUrl("https://modrepo.de/minecraft/voicechat/wiki/server_setup#testing-from-the-command-line");

        event.getInteraction().replyEmbeds(builder.build()).queue();
    }

}
