package de.maxhenkel.voicechatbot.support;

import de.maxhenkel.voicechatbot.Date;
import de.maxhenkel.voicechatbot.Environment;
import de.maxhenkel.voicechatbot.ExceptionHandler;
import de.maxhenkel.voicechatbot.Main;
import de.maxhenkel.voicechatbot.db.Thread;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SupportThreadUtils {

    public static Thread getThreadIfOwner(Interaction interaction) {
        Thread t = Main.DB.getThreadByUser(interaction.getUser().getIdLong());
        Channel channel = interaction.getChannel();
        if (channel == null || t == null || t.getThread() != channel.getIdLong()) {
            return null;
        }
        return t;
    }

    public static boolean isSupportThreadChannel(MessageChannel channel) {
        return channel.getIdLong() == Environment.SUPPORT_THREAD_CHANNEL_ID;
    }

    @Nullable
    public static TextChannel getChannel(long id) {
        return Main.API.getTextChannelById(id);
    }

    @Nullable
    public static ThreadChannel getThread(@Nullable Channel channel) {
        if (channel == null) {
            return null;
        }
        if (!(channel instanceof ThreadChannel thread)) {
            return null;
        }
        if (!isSupportThreadChannel(thread.getParentMessageChannel())) {
            return null;
        }
        return thread;
    }

    @Nullable
    public static ThreadChannel getThread(Interaction interaction) {
        return getThread(interaction.getChannel());
    }

    @Nullable
    public static Thread getThread(long user) {
        Thread thread = Main.DB.getThreadByUser(user);

        if (thread == null) {
            return null;
        }

        ThreadChannel threadChannel = Main.API.getThreadChannelById(thread.getThread());
        if (threadChannel == null) {
            Main.DB.removeThread(thread.getThread());
            Main.LOGGER.info("Removed thread {} of user {} as it doesn't exist anymore", thread.getThread(), thread.getUser());
            return null;
        }

        if (!isSupportThreadChannel(threadChannel.getParentMessageChannel())) {
            Main.DB.removeThread(thread.getThread());
            Main.LOGGER.info("Removed thread {} of user {} as it was not a child of the support channel", thread.getThread(), thread.getUser());
            return null;
        }

        if (threadChannel.isLocked()) {
            Main.DB.removeThread(thread.getThread());
            Main.LOGGER.info("Removed thread {} of user {} as it was locked", thread.getThread(), thread.getUser());
            return null;
        }
        return thread;
    }

    public static boolean isStaff(@Nullable Member member) {
        if (member == null) {
            return false;
        }
        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == Environment.SUPPORT_ROLE);
    }

    public static void closeThread(@Nullable ThreadChannel thread, @Nullable Thread t, User locker) {
        if (thread == null || t == null) {
            return;
        }
        thread.sendMessageEmbeds(
                new EmbedBuilder()
                        .setDescription("""
                                %s locked this thread.
                                You can always open a new one in <#%s> if you need help again.
                                """.formatted(locker.getAsMention(), Environment.SUPPORT_CHANNEL_ID))
                        .setColor(Color.RED)
                        .build()
        ).queue(message -> {
            Main.DB.removeThread(thread.getIdLong());

            thread.getManager()
                    .setArchived(true)
                    .setLocked(true)
                    .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                    .queue(
                            v -> updateStaffNotification(t, "Thread locked by %s".formatted(locker.getAsMention())),
                            new ExceptionHandler()
                    );
        }, new ExceptionHandler());
    }

    public static Button closeThreadButton() {
        return Button.danger(SupportThread.BUTTON_ABORT_SUPPORT, "I don't need help anymore");
    }

    public static CompletableFuture<Void> notifyStaff(ThreadChannel thread, Thread t) {
        TextChannel textChannel = Main.API.getTextChannelById(Environment.SUPPORT_NOTIFICATION_CHANNEL);
        if (textChannel == null) {
            Main.LOGGER.warn("Failed to find notification channel");
            return CompletableFuture.completedFuture(null);
        }

        if (t.getNotifyMessage() > 0) {
            updateStaffNotification(t, "Added staff again");
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("New Support Request")
                .addField("User", "<@%s>".formatted(t.getUser()), false)
                .addField("Thread", thread.getAsMention(), false)
                .setTimestamp(Instant.now())
                .setColor(Color.BLUE);

        textChannel.sendMessageEmbeds(embed.build())
                .queue(message -> {
                    Main.DB.setNotifyMessage(t.getThread(), message.getIdLong());
                    future.complete(null);
                }, error -> {
                    Main.LOGGER.error("Failed to send staff notification", error);
                    future.completeExceptionally(error);
                });

        return future;
    }

    public static CompletableFuture<Void> notifyStaff(ThreadChannel thread) {
        Thread t = Main.DB.getThread(thread.getIdLong());
        if (t == null) {
            return CompletableFuture.completedFuture(null);
        }
        return notifyStaff(thread, t);
    }

    public static void updateStaffNotification(Thread t, String message) {
        TextChannel textChannel = Main.API.getTextChannelById(Environment.SUPPORT_NOTIFICATION_CHANNEL);
        if (textChannel == null) {
            Main.LOGGER.warn("Failed to find notification channel");
            return;
        }

        if (t.getNotifyMessage() <= 0) {
            return;
        }
        textChannel.retrieveMessageById(t.getNotifyMessage())
                .queue(msg -> {
                    List<MessageEmbed> embeds = msg.getEmbeds();
                    if (embeds.isEmpty()) {
                        return;
                    }

                    MessageEmbed original = embeds.get(0);
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(original.getTitle())
                            .setDescription(original.getDescription())
                            .setColor(original.getColor())
                            .setTimestamp(original.getTimestamp());

                    for (MessageEmbed.Field field : original.getFields()) {
                        embed.addField(field);
                    }

                    embed.addField("Update %s UTC".formatted(Date.currentDate()), message, false);
                    msg.editMessageEmbeds(embed.build()).queue();
                }, new ExceptionHandler());
    }

}
