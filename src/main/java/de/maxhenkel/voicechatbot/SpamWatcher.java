package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SpamWatcher {
    private static final int TIME_WINDOW = 10_000;
    private static final int CHANNEL_COUNT = 3;

    private static final Map<Long, SpamData> SPAM_DATA = new ConcurrentHashMap<>();

    public static void onMessage(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        User user = event.getAuthor();
        Member member = event.getMember();
        if (member == null || user.isBot() || user.isSystem() || member.hasPermission(Permission.MODERATE_MEMBERS)) {
            return;
        }
        SpamData spamData = SPAM_DATA.computeIfAbsent(user.getIdLong(), k -> new SpamData());
        if (!spamData.addAndCheck(event.getChannel().getIdLong())) {
            return;
        }
        // Send DM before ban so bot and user have a shared server
        sendDm(event.getGuild(), user, () -> {
            member.ban(10, TimeUnit.MINUTES).queue(v -> {
                Main.LOGGER.info("(Soft) Banned user {} for spamming", user.getAsTag());
                event.getGuild().unban(user).queue(v1 -> {
                    Main.LOGGER.info("Unbanned user {} (Soft ban to delete messages and kick user)", user.getAsTag());
                }, new ExceptionHandler());
            }, new ExceptionHandler());
        });
        logSpam(user);
    }

    private static void sendDm(Guild guild, User user, Runnable onDone) {
        DefaultGuildChannelUnion defaultChannel = guild.getDefaultChannel();
        if (defaultChannel == null) {
            return;
        }
        defaultChannel.createInvite().setMaxAge(7L, TimeUnit.DAYS).setMaxUses(1).queue(invite -> {
            user.openPrivateChannel().queue(channel -> {
                channel.sendMessage("You have been kicked for spamming.\nYou can re-join the server immediately: %s".formatted(invite.getUrl())).queue(message -> {
                    onDone.run();
                }, t -> {
                    onDone.run();
                    new ExceptionHandler().accept(t);
                });
            }, t -> {
                onDone.run();
                new ExceptionHandler().accept(t);
            });
        }, t -> {
            onDone.run();
            new ExceptionHandler().accept(t);
        });
    }

    public static void logSpam(User user) {
        TextChannel logsChannel = Main.API.getTextChannelById(Environment.LOGS_CHANNEL);
        if (logsChannel == null) {
            Main.LOGGER.warn("Failed to find logs channel");
            return;
        }
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Spam detected")
                .setDescription("Soft banned user for spamming")
                .addField("User", user.getAsMention(), false)
                .setTimestamp(Instant.now())
                .setColor(Color.RED);
        logsChannel.sendMessageEmbeds(embed.build()).queue();
    }

    public static void cleanup() {
        long time = System.currentTimeMillis();
        SPAM_DATA.values().removeIf(spamData -> spamData.clean(time));
    }

    private static class SpamData {
        private final Map<Long, Long> channelTimes;

        public SpamData() {
            channelTimes = new ConcurrentHashMap<>();
        }

        public boolean addAndCheck(long channelId) {
            long time = System.currentTimeMillis();
            channelTimes.put(channelId, time);
            clean(time);
            return channelTimes.size() >= CHANNEL_COUNT;
        }

        /**
         * @param time the current time in milliseconds
         * @return true if this object can be cleaned up
         */
        public boolean clean(long time) {
            channelTimes.values().removeIf(t -> time - t > TIME_WINDOW);
            return channelTimes.isEmpty();
        }
    }

}
