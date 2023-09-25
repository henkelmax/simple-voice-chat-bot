package de.maxhenkel.voicechatbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

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
    private static final Set<Long> MODERATORS = new HashSet<>();
    private static final Map<Long, Integer> WARNED_USERS = new HashMap<>();

    public static void init(DiscordApi api) {
        if (Environment.NO_PING_ROLE <= 0L) {
            return;
        }
        api.getServers().forEach(PingWatcher::initServer);
    }

    private static void initServer(Server server) {
        server.getRoles()
                .stream()
                .filter(role -> !role.isEveryoneRole())
                .flatMap(role -> role.getUsers().stream().filter(user -> server.hasPermission(user, PermissionType.MODERATE_MEMBERS)).map(DiscordEntity::getId))
                .forEach(MODERATORS::add);
    }

    public static void onMessage(MessageCreateEvent event) {
        Optional<User> optionalUser = event.getMessageAuthor().asUser();
        if (optionalUser.isEmpty()) {
            return;
        }
        User user = optionalUser.get();
        Optional<Server> optionalServer = event.getServer();
        if (optionalServer.isEmpty()) {
            return;
        }
        Server server = optionalServer.get();

        if (user.isBot() || isModerator(user)) {
            return;
        }
        List<User> mentionedUsers = event.getMessage().getMentionedUsers();

        if (mentionedUsers.stream().noneMatch(user1 -> isNoPing(user1, server))) {
            return;
        }

        String message = event.getMessage().getContent();

        Matcher matcher = MENTION_REGEX.matcher(message);

        List<User> pingedUsers = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            server.getMemberById(id).ifPresent(pingedUsers::add);
        }

        int warningAmount = WARNED_USERS.getOrDefault(user.getId(), 0) + 1;
        WARNED_USERS.put(user.getId(), warningAmount);

        if (warningAmount <= 3 && pingedUsers.stream().noneMatch(user1 -> isNoPing(user1, server))) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setDescription("<@%s>!\nPlease disable pings when replying to admins or moderators!".formatted(user.getId()));
            builder.setColor(Color.ORANGE);
            event.getMessage().reply(builder);
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription("<@%s>!\nDon't ping admins or moderators!".formatted(user.getId()));
        builder.setColor(Color.RED);
        event.getMessage().reply(builder);

        user.timeout(server, Duration.of(1, ChronoUnit.HOURS), "Pinging admins or moderators");
    }

    private static boolean isModerator(User user) {
        return MODERATORS.contains(user.getId());
    }

    private static boolean isNoPing(User user, @Nullable Server server) {
        if (server == null) {
            return false;
        }
        return user.getRoles(server).stream().anyMatch(role -> role.getId() == Environment.NO_PING_ROLE);
    }

}
