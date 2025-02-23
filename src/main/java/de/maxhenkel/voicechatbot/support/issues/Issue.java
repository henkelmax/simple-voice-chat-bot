package de.maxhenkel.voicechatbot.support.issues;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;

public interface Issue {

    String getId();

    String getName();

    List<String> getQuestions();

    void onSelectIssue(ThreadChannel textChannel);

    void sendQuestions(ThreadChannel thread);

}
