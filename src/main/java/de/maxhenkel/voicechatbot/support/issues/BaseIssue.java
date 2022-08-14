package de.maxhenkel.voicechatbot.support.issues;

import de.maxhenkel.voicechatbot.Main;
import de.maxhenkel.voicechatbot.support.SupportThread;
import de.maxhenkel.voicechatbot.support.SupportThreadUtils;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.ButtonBuilder;
import org.javacord.api.entity.message.component.ButtonStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaseIssue implements Issue {

    private final String id;
    private final String name;
    protected List<String> questions;

    public BaseIssue(String id, String name) {
        this.id = id;
        this.name = name;
        this.questions = getQuestionsInternal();
    }

    protected List<String> getQuestionsInternal() {
        List<String> questions = new ArrayList<>();
        questions.add("What is your issue?");
        questions.add("What Minecraft version are you using?");
        questions.add("What voice chat mod version are you using on your game? *(Please post the full filename of the mod jar)*");
        questions.add("What voice chat mod/plugin version are you using on your server? *(Please post the full filename of the mod/plugin jar)*");
        return questions;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getQuestions() {
        return questions;
    }

    @Override
    public void onSelectIssue(TextChannel textChannel) {

    }

    @Override
    public void sendQuestions(ServerThreadChannel thread) {
        thread.sendMessage(new EmbedBuilder()
                        .setTitle("Please answer the following questions")
                        .setDescription("""
                                You can just send the answers as normal text messages in this thread.
                                                                    
                                %s
                                                                    
                                Once you answered every question, confirm them by pressing the `Confirm` button.
                                """.formatted(getQuestions().stream().map("⦁ %s"::formatted).collect(Collectors.joining("\n"))))
                        .setColor(Color.BLUE),
                ActionRow.of(
                        SupportThreadUtils.closeThreadButton(),
                        new ButtonBuilder().setCustomId(SupportThread.BUTTON_CONFIRM_ANSWERS).setLabel("Confirm").setStyle(ButtonStyle.SUCCESS).build()
                )
        ).thenAccept(message -> {
            Main.DB.unlockThread(thread.getId());
        });
    }
}
