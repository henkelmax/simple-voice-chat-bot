package de.maxhenkel.voicechatbot.support.issues;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

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
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you encountered a crash, please provide log files of both your client and server!
                                                    
                        Please send both logs as a file in this channel.
                        """)
                .addField("Client logs", "`.minecraft/logs/latest.log`")
                .addField("Server logs", "`logs/latest.log`")
                .setColor(Color.RED)
        );
    }
}
