package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.awt.*;
import java.util.List;

public class VoiceChoppyIssue extends BaseIssue {

    public VoiceChoppyIssue() {
        super("issue_voice_choppy", "Voice chat sounds choppy");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("What server software are you using? *(Fabric, Forge, Bukkit, Spigot, Paper etc.)*");
        questions.add("Are you using a proxy server? *(BungeeCord, Waterfall, Velocity etc.)*");
        questions.add("Are you using any DDoS protection? *(TCPShield etc)*");
        questions.add("Where are you hosting your server? *(Bloom, Aternos, Own PC, VPS etc.)*");
        questions.add("Are you using any tunneling service/mod or VPN? *(playit.gg, e4mc, ngrok, ZeroTier, Radmin, Hamachi etc.)*");
        return questions;
    }

    @Override
    public void onSelectIssue(ThreadChannel textChannel) {
        textChannel.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you are using a DDoS protection, please check if it is limiting/blocking the voice chat UDP packets.
                        
                        In case you are hosting your server on **BisectHosting**, make sure you followed their [setup guide](https://modrepo.de/minecraft/voicechat/wiki/server_hosting/bisecthosting).
                        Note this will only work if you are using BisectHosting premium.
                        **The voice chat won't work on BisectHosting budget servers!**
                        """)
                .setColor(Color.RED)
                .build()
        ).queue();
    }
}
