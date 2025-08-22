package de.maxhenkel.voicechatbot.portchecker;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MessageAppender {

    @Nullable
    private Message messageUpdater;
    @Nullable
    private EmbedBuilder builder;
    @Nullable
    private EmbedBuilder finalEmbed;

    private final MessageChannelUnion channel;
    private final String url;
    private final List<String> logs;

    public MessageAppender(MessageChannelUnion channel, String url) {
        this.channel = channel;
        this.url = url;
        this.logs = new ArrayList<>();
    }

    public MessageAppender startEmbed() {
        builder = new EmbedBuilder();
        builder.setTitle("Port Checker");
        builder.setColor(Color.BLUE);
        builder.setDescription(constructBody());

        channel.sendMessageEmbeds(builder.build()).queue(message -> setUpdater(message));
        return this;
    }

    public MessageAppender addLog(String text) {
        logs.add(text);
        return this;
    }

    public MessageAppender addFinalEmbed(EmbedBuilder embed) {
        finalEmbed = embed;
        return this;
    }

    public MessageAppender finishWithError(String message) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Check failed with an error");
        builder.setDescription(message);
        builder.setColor(Color.RED);
        addFinalEmbed(builder);
        return this;
    }

    public MessageAppender finishWithUnsuccessful(String message) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Check unsuccessful");
        builder.setDescription(message);
        builder.setColor(Color.RED);
        addFinalEmbed(builder);
        return this;
    }

    public MessageAppender finishWithUnstable(String message) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Unstable connection");
        builder.setDescription(message);
        builder.setColor(Color.ORANGE);
        addFinalEmbed(builder);
        return this;
    }

    public MessageAppender finishSuccess(String message) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Check successful");
        builder.setDescription(message);
        builder.setColor(Color.GREEN);
        addFinalEmbed(builder);
        return this;
    }

    private void setUpdater(Message updater) {
        messageUpdater = updater;
        updateMessage();
    }

    public MessageAppender updateMessage() {
        if (messageUpdater == null || builder == null) {
            return this;
        }
        builder.setDescription(constructBody());

        if (finalEmbed != null) {
            messageUpdater.editMessageEmbeds(builder.build(), finalEmbed.build()).queue();
        } else {
            messageUpdater.editMessageEmbeds(builder.build()).queue();
        }
        return this;
    }

    private String constructBody() {
        StringBuilder sb = new StringBuilder();

        sb.append("Checking if the port of voice chat server `");
        sb.append(url);
        sb.append("` is open.");
        sb.append("\n\n");

        if (!logs.isEmpty()) {
            sb.append("```");
            for (String log : logs) {
                sb.append(log);
                sb.append("\n");
            }
            sb.append("```");
        }
        return sb.toString();
    }

}
