package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

public class QuestionCommand {

    private static final String QUESTION_COMMAND = "question";

    public static void init() {
        CommandRegistry.registerCommand(QUESTION_COMMAND, "Tells a user to use support threads",
                Collections.singletonList(new OptionData(OptionType.USER, "user", "The user to inform", true)),
                QuestionCommand::onQuestionCommand,
                Permission.MODERATE_MEMBERS
        );
    }

    private static void onQuestionCommand(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("user");
        if (option == null) {
            event.reply("Missing required input!").setEphemeral(true).queue();
            return;
        }

        User user = option.getAsUser();

        TextChannel channel = event.getInteraction().getChannel().asTextChannel();

        event.deferReply(true).queue(hook -> {
            channel.getHistory().retrievePast(16).queue(messages -> {
                Optional<Message> optionalMessage = messages.stream()
                        .filter(message -> message.getAuthor().getIdLong() == user.getIdLong())
                        .max(Comparator.comparing(ISnowflake::getTimeCreated));

                if (optionalMessage.isPresent()) {
                    Message message = optionalMessage.get();
                    TextChannel supportChannel = event.getJDA().getTextChannelById(Environment.SUPPORT_CHANNEL_ID);

                    if (supportChannel == null) {
                        event.reply("Support channel not found").setEphemeral(true).queue();
                        return;
                    }

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setDescription("This channel is not the appropriate place to ask questions %s. Please open a support ticket in %s and select the issue type `General Question`."
                            .formatted(user.getAsMention(), supportChannel.getAsMention()));

                    message.replyEmbeds(builder.build()).queue();
                    hook.deleteOriginal().queue();
                } else {
                    event.reply("No messages found from user").setEphemeral(true).queue();
                }
            }, new ExceptionHandler());
        }, new ExceptionHandler());
    }

}
