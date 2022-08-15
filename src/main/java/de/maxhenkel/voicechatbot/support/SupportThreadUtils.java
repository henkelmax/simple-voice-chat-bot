package de.maxhenkel.voicechatbot.support;

import de.maxhenkel.voicechatbot.Date;
import de.maxhenkel.voicechatbot.Environment;
import de.maxhenkel.voicechatbot.ExceptionHandler;
import de.maxhenkel.voicechatbot.Main;
import de.maxhenkel.voicechatbot.db.Thread;
import org.javacord.api.entity.channel.AutoArchiveDuration;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.ButtonBuilder;
import org.javacord.api.entity.message.component.ButtonStyle;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SupportThreadUtils {

    public static Thread getThreadIfOwner(InteractionBase interactionBase) {
        Thread t = Main.DB.getThreadByUser(interactionBase.getUser().getId());
        TextChannel channel = interactionBase.getChannel().orElse(null);
        if (channel == null || t == null || t.getThread() != channel.getId()) {
            return null;
        }
        return t;
    }

    public static boolean isSupportChannel(Channel channel) {
        return channel.getId() == Environment.SUPPORT_CHANNEL_ID;
    }

    @Nullable
    public static ServerThreadChannel getThread(@Nullable Channel channel) {
        if (channel == null) {
            return null;
        }
        ServerThreadChannel thread = channel.asServerThreadChannel().orElse(null);
        if (thread == null) {
            return null;
        }
        if (!isSupportChannel(thread.getParent())) {
            return null;
        }
        return thread;
    }

    @Nullable
    public static ServerThreadChannel getThread(InteractionBase interactionBase) {
        return getThread(interactionBase.getChannel().orElse(null));
    }

    @Nullable
    public static Thread getThread(long user) {
        Thread thread = Main.DB.getThreadByUser(user);

        if (thread == null) {
            return null;
        }

        Channel c = Main.API.getChannelById(thread.getThread()).orElse(null);
        if (c == null) {
            Main.DB.removeThread(thread.getThread());
            Main.LOGGER.info("Removed thread {} of user {} as it doesn't exist anymore", thread.getThread(), thread.getUser());
            return null;
        }

        ServerThreadChannel threadChannel = c.asServerThreadChannel().orElse(null);

        if (threadChannel == null) {
            Main.DB.removeThread(thread.getThread());
            Main.LOGGER.info("Removed thread {} of user {} as it isn't actually a thread", thread.getThread(), thread.getUser());
            return null;
        }

        if (!isSupportChannel(threadChannel.getParent())) {
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

    public static boolean isStaff(MessageAuthor author) {
        User user = author.asUser().orElse(null);
        if (user == null) {
            return false;
        }
        Server server = author.getMessage().getServer().orElse(null);
        if (server == null) {
            return false;
        }
        return isStaff(user, server);
    }

    public static boolean isStaff(User user, @Nullable Server server) {
        if (server == null) {
            return false;
        }
        return user.getRoles(server).stream().anyMatch(role -> role.getId() == Environment.SUPPORT_ROLE);
    }

    public static void closeThread(@Nullable ServerThreadChannel thread, @Nullable Thread t, long locker) {
        if (thread == null || t == null) {
            return;
        }
        thread.sendMessage(new EmbedBuilder().setDescription("<@%s> locked this thread.".formatted(locker)).setColor(Color.RED)).thenAccept(message -> {
            Main.DB.removeThread(thread.getId());
            thread.createUpdater().setArchivedFlag(true).setLockedFlag(true).setAutoArchiveDuration(AutoArchiveDuration.ONE_HOUR).update();
        }).exceptionally(new ExceptionHandler<>());
        updateStaffNotification(t, "Thread locked by <@%s>".formatted(locker));
    }

    public static Button closeThreadButton() {
        return new ButtonBuilder().setCustomId(SupportThread.BUTTON_ABORT_SUPPORT).setLabel("I don't need help anymore").setStyle(ButtonStyle.DANGER).build();
    }

    public static CompletableFuture<Void> notifyStaff(ServerThreadChannel thread, Thread t) {
        Channel c = Main.API.getChannelById(Environment.SUPPORT_NOTIFICATION_CHANNEL).orElse(null);
        if (c == null) {
            Main.LOGGER.warn("Failed to find notification channel");
            return CompletableFuture.completedFuture(null);
        }
        TextChannel textChannel = c.asTextChannel().orElse(null);
        if (textChannel == null) {
            Main.LOGGER.warn("Notification channel is not a text channel");
            return CompletableFuture.completedFuture(null);
        }

        if (t.getNotifyMessage() > 0) {
            updateStaffNotification(t, "Added staff again");
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("New Support Request")
                .addField("User", "<@%s>".formatted(t.getUser()))
                .addField("Thread", "<#%s>".formatted(thread.getId()))
                .setTimestampToNow()
                .setColor(Color.BLUE)
        ).thenAccept(message -> {
            Main.DB.setNotifyMessage(t.getThread(), message.getId());
            future.complete(null);
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    public static CompletableFuture<Void> notifyStaff(ServerThreadChannel thread) {
        Thread t = Main.DB.getThread(thread.getId());
        if (t == null) {
            return CompletableFuture.completedFuture(null);
        }
        return notifyStaff(thread, t);
    }

    public static void updateStaffNotification(Thread t, String message) {
        Channel c = Main.API.getChannelById(Environment.SUPPORT_NOTIFICATION_CHANNEL).orElse(null);
        if (c == null) {
            Main.LOGGER.warn("Failed to find notification channel");
            return;
        }
        TextChannel textChannel = c.asTextChannel().orElse(null);
        if (textChannel == null) {
            Main.LOGGER.warn("Notification channel is not a text channel");
            return;
        }

        if (t.getNotifyMessage() <= 0) {
            return;
        }
        Main.API.getMessageById(t.getNotifyMessage(), textChannel).thenAccept(msg -> {
            List<Embed> embeds = msg.getEmbeds();
            if (embeds.isEmpty()) {
                return;
            }
            EmbedBuilder embed = embeds.get(0).toBuilder();
            embed.addField("Update %s UTC".formatted(Date.currentDate()), message);

            msg.createUpdater().setEmbed(embed).applyChanges().exceptionally(new ExceptionHandler<>());
        }).exceptionally(new ExceptionHandler<>());
    }

}
