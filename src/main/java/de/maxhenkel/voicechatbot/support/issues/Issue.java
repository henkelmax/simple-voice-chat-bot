package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import javax.annotation.Nullable;
import java.util.List;

public interface Issue {

    String getId();

    String getName();

    List<String> getQuestions();

    @Nullable
    MessageEmbed getDisclaimer();

    void sendQuestions(ThreadChannel thread);

}
