package de.maxhenkel.voicechatbot.support.issues;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class PortInUseIssue extends BaseIssue {

    public PortInUseIssue() {
        super("issue_port_in_use", "Voice chat port already in use");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("What server software are you using? *(Fabric/Forge/Bukkit/Spigot/Paper etc)*");
        questions.add("Are you running any other Minecraft servers on that machine?");
        questions.add("Where are you hosting your server? *(Bloom/Aternos/Own PC/VPS etc)*");
        questions.add("Which port are you using for the voice chat?");
        questions.add("Do you have server query enabled? *(`enable-query` option in server.properties)*");
        questions.add("On which port is your server query running? *(`query.port` option in server.properties)*");
        questions.add("What other mods/plugins are you using?");
        return questions;
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If the voice chat port is already in use, please check the following:
                        - Check if there are any other instances of the voice chat mod/plugin running that use the same port.
                        - If you are using other mods/plugins that need a UDP port like GeyserMC, make sure they are using a different port than the voice chat.
                        - Make sure the server query is not running on the same port as the voice chat. You can find the query port in your server.properties (`query.port`).
                        """)
                .setColor(Color.RED)
        );
    }
}
