package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.awt.*;
import java.util.List;

public class NotConnectedIssue extends BaseIssue {

    public NotConnectedIssue() {
        super("issue_not_connected", "Voice chat not connected");
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
                        If you are hosting your server with a Minecraft hosting provider, please do the following:
                        - Visit [this page](https://modrepo.de/minecraft/voicechat/wiki/server_setup_mc_hosting) to see if instructions exist for your specific hoster.
                        - **If no guide is available**, reach out to your hosting providers support team directly for assistance.
                        - **If the guide doesn't work**, contact your hosters support team first to resolve the issue.
                        
                        **Important Notes**:
                        - We cannot assist with configuration or port setup for individual hosting providers. Due to the unique requirements of each service, please contact your hosters support team directly for guidance.
                        - **Hybrid servers** (e.g., Mohist, Magma, Arclight) are not officially supported due to compatibility and stability risks. [Learn why here](https://essentialsx.net/do-not-use-mohist.html).
                        - **Tunneling services** (e.g., playit.gg, e4mc, ngrok, Localtonet) and **VPNs** (e.g., ZeroTier, Radmin, Hamachi) are not officially supported, as these setups are overly complex, require advanced technical expertise, and are prone to configuration errors.
                        - For reliable performance, always configure port forwarding directly through your hosting provider or router instead of relying on solutions like tunneling services or VPNs.
                        """)
                .setColor(Color.RED)
                .build()
        ).queue();
    }
}
