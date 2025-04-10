package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingWatcher {

    private static final Pattern MENTION_REGEX = Pattern.compile("<@(\\d+)>");
    private static final Map<Long, Integer> WARNED_USERS = new HashMap<>();

    public static void onMessage(MessageReceivedEvent event) {
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

        String message = event.getMessage().getContentRaw();

        Matcher matcher = MENTION_REGEX.matcher(message);

        List<Member> pingedMembers = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            Member pingedMember = server.getMemberById(id);
            if (pingedMember != null) {
                pingedMembers.add(pingedMember);
            }
        }

        int warningAmount = WARNED_USERS.getOrDefault(user.getIdLong(), 0) + 1;
        WARNED_USERS.put(user.getIdLong(), warningAmount);

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

}
