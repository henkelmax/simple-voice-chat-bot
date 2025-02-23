package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.awt.*;
import java.util.List;

public class MicNotWorkingIssue extends BaseIssue {

    public MicNotWorkingIssue() {
        super("issue_mic_not_working", "Microphone not working");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("Which operating system are you using? *(Windows/MacOS/Linux)*");
        questions.add("Which operating system version are you using? (MacOS 10.15/Windows 10 etc)");
        return questions;
    }

    @Override
    public void onSelectIssue(ThreadChannel textChannel) {
        textChannel.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you are on **MacOS**, you need to use [Prism Launcher](https://prismlauncher.org/) to get access to your microphone!
                        For more information read [this](https://modrepo.de/minecraft/voicechat/wiki/macos).
                        """)
                .setColor(Color.RED)
                .build()
        ).queue();
    }
}
