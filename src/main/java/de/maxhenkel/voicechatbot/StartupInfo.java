package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.*;
import java.time.Instant;

public class StartupInfo {

    public static void logStarted() {
        TextChannel textChannel = Main.API.getTextChannelById(Environment.SUPPORT_NOTIFICATION_CHANNEL);
        if (textChannel == null) {
            Main.LOGGER.warn("Failed to find notification channel");
            return;
        }
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Bot started")
                .addField("Version", VersionCommand.getVersion(), false)
                .addField("Startup time", TimeFormat.DATE_TIME_SHORT.format(Instant.now()), false)
                .setTimestamp(Instant.now())
                .setColor(Color.GREEN);
        textChannel.sendMessageEmbeds(embed.build()).queue();
    }

}
