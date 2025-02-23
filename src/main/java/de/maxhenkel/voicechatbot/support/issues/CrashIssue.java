package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.awt.*;
import java.util.List;

public class CrashIssue extends BaseIssue {

    public CrashIssue() {
        super("issue_crash", "Crash");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("Did the crash occur on the client or the server?");
        return questions;
    }

    @Override
    public void onSelectIssue(ThreadChannel textChannel) {
        textChannel.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you encountered a crash, please provide log files of both your client and server!
                        
                        Please send both logs as a file in this channel.
                        """)
                .addField("Client logs", "`.minecraft/logs/latest.log`", false)
                .addField("Server logs", "`logs/latest.log`", false)
                .setColor(Color.RED)
                .build()
        ).queue();
    }
}
