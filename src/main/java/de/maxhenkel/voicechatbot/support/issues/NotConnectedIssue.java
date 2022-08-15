package de.maxhenkel.voicechatbot.support.issues;

import de.maxhenkel.voicechatbot.Environment;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class NotConnectedIssue extends BaseIssue {

    public NotConnectedIssue() {
        super("issue_not_connected", "Voice chat not connected");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("What server software are you using? *(Fabric/Forge/Bukkit/Spigot/Paper etc)*");
        questions.add("Are you using a proxy server? *(Bungeecord/Waterfall/Velocity etc)*");
        questions.add("Are you using any DDoS protection? *(TCPShield etc)*");
        questions.add("Where are you hosting your server? *(Bloom/Aternos/Own PC/VPS etc)*");
        return questions;
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you are hosting your server with a Minecraft hosting provider, please do the following:
                                                    
                        ⦁ Go to <#%s> and look if a guide for your hoster exists
                        ⦁ If there is no guide for your hoster, please **contact the support of your hoster**
                        ⦁ If you found a guide for your hoster in <#%s>, but it doesn't work, please also contact your hoster first
                                                    
                        **We can't help you with the configuration for specific Minecraft hosters! Please always contact their support first!**
                        
                        You should also know that we generally don't support hybrid servers like Mohist or Magma.
                        Read [this](https://essentialsx.net/do-not-use-mohist.html) for more information.
                        
                        Tools like ngrok also won't work, since they only support TCP.
                        """.formatted(Environment.SERVER_HOSTING_CHANNEL_ID, Environment.SERVER_HOSTING_CHANNEL_ID))
                .setColor(Color.RED)
        );
    }
}
