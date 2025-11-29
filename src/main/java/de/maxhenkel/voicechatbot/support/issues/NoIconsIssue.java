package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.List;

public class NoIconsIssue extends BaseIssue {

    public NoIconsIssue() {
        super("issue_no_icons", "No voice chat icons");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("Which launcher are you using? *(Vanilla Launcher, Prism Launcher, CurseForge Launcher, ATLauncher etc.)*");
        questions.add("Did you check that the icons aren't hidden? *(Check the disclaimer)*");
        questions.add("Does this happen without any other mods installed?");
        return questions;
    }

    @Override
    public MessageEmbed getDisclaimer() {
        return new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        Make sure your icons aren't hidden.
                        Please check the following [client config](https://modrepo.de/minecraft/voicechat/wiki/client_config) options:
                        - `hide_icons` needs to be `false`
                        - `show_nametag_icons` needs to be `true`
                        - `show_hud_icons` needs to be `true`
                        - `show_group_hud` needs to be `true`
                        
                        Also note that we don't provide support for custom clients like Lunar or Feather.
                        """)
                .setColor(Color.RED)
                .build();
    }
}
