package de.maxhenkel.voicechatbot.support.issues;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class MicNotWorkingIssue extends BaseIssue {

    public MicNotWorkingIssue() {
        super("issue_mic_not_working", "Microphone not working");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.add("Operating system are you using? *(Windows/MacOS/Linux)*");
        questions.add("Operating system version are you on? (MacOS 10.15/Windows 10 etc)");
        return questions;
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Disclaimer")
                .setDescription("""
                        If you are on **MacOS**, you need to patch your launcher in order to get your microphone working!
                                                    
                        ⦁ If you don't know how to patch your launcher, read [this](https://github.com/henkelmax/simple-voice-chat/tree/1.19.2/macos)
                        ⦁ If there is no patcher popping up when launching your game, download the [standalone patcher](https://github.com/henkelmax/simple-voice-chat/tree/1.19.2/macos#standalone-version)
                                                    
                        """)
                .setColor(Color.RED)
        );
    }
}
