package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class PingWatcher {

    public static void onMessage(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        User user = event.getAuthor();
        Guild server = event.getGuild();
        Member member = event.getMember();
        if (member == null) {
            return;
        }

        if (user.isBot() || user.isSystem() || member.hasPermission(Permission.MODERATE_MEMBERS)) {
            return;
        }
        List<Member> mentionedMembers = event.getMessage().getMentions().getMembers();

        if (mentionedMembers.stream().noneMatch(m -> isNoPing(m, server))) {
            return;
        }

        Message referenced = event.getMessage().getReferencedMessage();
        long repliedAuthorId = referenced == null ? -1L : referenced.getAuthor().getIdLong();

        List<Member> pingedMembers = mentionedMembers.stream()
                .filter(m -> m.getIdLong() != repliedAuthorId)
                .toList();

        int warningAmount = Main.DB.getAndIncreasePings(user.getIdLong());

        if (warningAmount <= 3 && pingedMembers.stream().noneMatch(m -> isNoPing(m, server))) {
            member.timeoutFor(Duration.of(10L * warningAmount, ChronoUnit.SECONDS)).reason("Pinging admins or moderators").queue();
            EmbedBuilder builder = new EmbedBuilder();
            builder.setDescription("%s!\nPlease disable pings when replying to admins or moderators!".formatted(user.getAsMention()));
            builder.setColor(Color.ORANGE);
            event.getMessage().replyEmbeds(builder.build()).queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription("%s!\nDon't ping admins or moderators!".formatted(user.getAsMention()));
        builder.setColor(Color.RED);
        event.getMessage().replyEmbeds(builder.build()).queue();

        member.timeoutFor(Duration.of(Math.max(warningAmount - 3L, 1L), ChronoUnit.HOURS)).reason("Pinging admins or moderators").queue();
    }

    private static boolean isNoPing(Member member, @Nullable Guild server) {
        if (server == null) {
            return false;
        }
        if (Environment.NO_PING_ROLE <= 0L) {
            return false;
        }
        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == Environment.NO_PING_ROLE);
    }

    public static void cleanupPings() {
        Main.DB.cleanupPings();
    }
}
