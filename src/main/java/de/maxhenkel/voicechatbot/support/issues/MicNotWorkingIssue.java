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
        questions.add("Which launcher are you using? *(Vanilla Launcher, Prism Launcher, CurseForge Launcher, ATLauncher etc.)*");
        questions.add("Which operating system are you using? *(Windows, MacOS, Linux etc.)*");
        questions.add("Which operating system version are you using? (MacOS 14, Windows 11 etc.)");
        return questions;
    }

    @Override
    public void onSelectIssue(ThreadChannel textChannel) {
        textChannel.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you are on **MacOS**, you need to use [Prism Launcher](https://prismlauncher.org/) or any other supported launcher to get access to your microphone!
                        For more information read [this](https://modrepo.de/minecraft/voicechat/wiki/macos).
                        """)
                .setColor(Color.RED)
                .build()
        ).queue();
    }
}
