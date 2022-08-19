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
        questions.add("What other mods/plugins are you using?");
        return questions;
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If the voice chat port is already in use, please check if there are any other instances of the mod running that use the same port.
                        If you are using other mods/plugins that need a UDP port like GeyserMC, make sure they are using a different port than the voice chat.
                        If you are running the voice chat off of port 25565, make sure server query is not running at that port.
                        """)
                .setColor(Color.RED)
        );
    }
}
