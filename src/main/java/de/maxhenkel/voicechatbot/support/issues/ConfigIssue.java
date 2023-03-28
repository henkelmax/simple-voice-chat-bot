package de.maxhenkel.voicechatbot.support.issues;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;

public class ConfigIssue extends BaseIssue {

    public ConfigIssue() {
        super("issue_config", "Can't find the config file");
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        While editing configuration files, make sure the client/server is stopped.
                        If the config values keep resetting, this is most likely the problem.
                        If you can't find the config files, make sure the client/server was started at least once, so that the files are generated.
                        
                        If you are using an older version of the mod (older than 2.4.0) read [this](https://modrepo.de/minecraft/voicechat/wiki/old_configuration).
                        """)
                .addField("Fabric/Forge/Quilt config location", "*Server*:\n`config/voicechat/voicechat-server.properties`\n*Client*:\n`config/voicechat/voicechat-client.properties`")
                .addField("Bukkit/Spigot/Paper config location", "`plugins/voicechat/voicechat-server.properties`")
                .setColor(Color.RED)
        );
    }
}
