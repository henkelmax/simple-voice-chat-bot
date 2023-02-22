package de.maxhenkel.voicechatbot;

import de.maxhenkel.voicechatbot.support.SupportThreadUtils;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class QuestionCommand {

    private static final String QUESTION_COMMAND = "question";

    public static void init() {
        CommandRegistry.registerCommand(QUESTION_COMMAND, "Tells a user to use support threads",
                Collections.singletonList(SlashCommandOption.createUserOption(
                        "user",
                        "The user to inform",
                        true
                )),
                QuestionCommand::onQuestionCommand,
                PermissionType.MODERATE_MEMBERS
        );
    }

    private static void onQuestionCommand(SlashCommandCreateEvent event) {
        List<SlashCommandInteractionOption> arguments = event.getSlashCommandInteraction().getArguments();
        if (arguments.size() != 1) {
            return;
        }
        User user = arguments.get(0).getUserValue().orElse(null);
        if (user == null) {
            return;
        }

        TextChannel channel = event.getSlashCommandInteraction().getChannel().orElse(null);
        if (channel == null) {
            return;
        }

        event.getSlashCommandInteraction().respondLater().thenAccept(responseUpdater -> {
            channel.getMessages(16).thenAccept(messages -> {
                Optional<Message> optionalMessage = messages.stream().filter(message -> message.getAuthor().getId() == user.getId()).max(Comparator.comparing(DiscordEntity::getCreationTimestamp));
                if (optionalMessage.isPresent()) {
                    Message message = optionalMessage.get();
                    EmbedBuilder builder = new EmbedBuilder();
                    ServerTextChannel supportChannel = SupportThreadUtils.getChannel(Environment.SUPPORT_CHANNEL_ID);
                    if (supportChannel == null) {
                        responseUpdater.setContent("Support channel not found").setFlags(MessageFlag.EPHEMERAL).update().exceptionally(new ExceptionHandler<>());
                        return;
                    }
                    builder.setDescription("This channel is not the appropriate place to ask questions. Please open a support ticket in <#%s> and select the issue type `General Question`.".formatted(supportChannel.getId()));
                    message.reply(builder);

                    responseUpdater.delete().exceptionally(new ExceptionHandler<>());
                } else {
                    responseUpdater.setContent("No messages found from user").setFlags(MessageFlag.EPHEMERAL).update().exceptionally(new ExceptionHandler<>());
                }
            }).exceptionally(new ExceptionHandler<>());
        }).exceptionally(new ExceptionHandler<>());
    }

}
