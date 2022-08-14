package de.maxhenkel.voicechatbot.support.issues;

import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.channel.TextChannel;

import java.util.List;

public interface Issue {

    String getId();

    String getName();

    List<String> getQuestions();

    void onSelectIssue(TextChannel textChannel);

    void sendQuestions(ServerThreadChannel thread);

}
