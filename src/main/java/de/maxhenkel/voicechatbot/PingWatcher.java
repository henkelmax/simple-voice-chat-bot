package de.maxhenkel.voicechatbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PingWatcher {

    private static final Set<Long> MODERATORS = new HashSet<>();

    public static void init(DiscordApi api) {
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

        if (mentionedUsers.stream().noneMatch(PingWatcher::isModerator)) {
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

}
