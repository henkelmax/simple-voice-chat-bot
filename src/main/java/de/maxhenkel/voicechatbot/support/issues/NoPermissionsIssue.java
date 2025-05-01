package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.List;

public class NoPermissionsIssue extends BaseIssue {

    public NoPermissionsIssue() {
        super("issue_no_permissions", "No voice chat permissions");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("What server software are you using? *(Fabric, NeoForge, Forge, Bukkit, Paper etc.)*");
        questions.add("Are you using any permission manager mod/plugin? *(LuckPerms, PermissionsEx etc.)*");
        return questions;
    }

    @Override
    public MessageEmbed getDisclaimer() {
        return new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        Please make sure you granted the following permissions for every player:
                        
                        - `voicechat.listen`
                        - `voicechat.speak`
                        - `voicechat.groups`
                        
                        If you don't know how to grant permissions for your permissions mod/plugin, please contact their support.
                        We can't help you with the setup of other mods.
                        
                        For more information read [this](https://modrepo.de/minecraft/voicechat/wiki/permissions).
                        
                        You should also know that we generally don't support hybrid servers like Mohist, Magma or Arclight.
                        Read [this](https://essentialsx.net/do-not-use-mohist.html) for more information.
                        """)
                .setColor(Color.RED)
                .build();
    }

}
