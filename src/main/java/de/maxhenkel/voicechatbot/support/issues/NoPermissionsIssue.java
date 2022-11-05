package de.maxhenkel.voicechatbot.support.issues;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class NoPermissionsIssue extends BaseIssue {

    public NoPermissionsIssue() {
        super("issue_no_permissions", "No voice chat permissions");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("What server software are you using? *(Fabric/Forge/Bukkit/Spigot/Paper etc)*");
        questions.add("Are you using any permission manager mod/plugin? *(LuckPerms/PermissionsEx etc)*");
        return questions;
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        Please make sure you granted the following permissions for every player:
                                                    
                        ⦁ ~~`voicechat.connect`~~ *Removed in version 2.3.13*
                        ⦁ `voicechat.listen` *Added in version 2.3.13*
                        ⦁ `voicechat.speak`
                        ⦁ `voicechat.groups`
                                                
                        If you don't know how to grant permissions for your permissions mod/plugin, please contact their support.
                        We can't help you with the setup of other mods.
                        
                        For more information read [this](https://modrepo.de/minecraft/voicechat/wiki?t=permissions).
                        
                        You should also know that we generally don't support hybrid servers like Mohist or Magma.
                        Read [this](https://essentialsx.net/do-not-use-mohist.html) for more information.
                        """)
                .setColor(Color.RED)
        );
    }
}
