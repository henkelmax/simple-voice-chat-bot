package de.maxhenkel.voicechatbot;

import de.maxhenkel.voicechatbot.db.Database;
import de.maxhenkel.voicechatbot.support.SupportThread;
import de.maxhenkel.voicechatbot.support.ThreadCooldown;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static DiscordApi API;
    public static Database DB;
    public static ScheduledExecutorService EXECUTOR;

    public static final Logger LOGGER = LogManager.getLogger("voicechatbot");

    public static void main(String[] args) {
        LOGGER.info("Starting bot");

        if (!Environment.validate()) {
            return;
        }

        EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        DB = new Database();
        API = new DiscordApiBuilder().setToken(Environment.TOKEN).setAllIntents().login().join();
        LOGGER.info("Logged in");

        LOGGER.info("Setting activity");
        API.updateActivity(ActivityType.WATCHING, "Support");

        ButtonRegistry.init();
        CommandRegistry.init();

        API.addMessageCreateListener(SupportThread::onMessage);
        API.addModalSubmitListener(SupportThread::onModalSubmit);
        API.addSelectMenuChooseListener(SupportThread::onSelectMenuChoose);
        API.addMessageCreateListener(LogUploader::onMessage);
        if (Environment.NO_PING_ROLE > 0L) {
            API.addMessageCreateListener(PingWatcher::onMessage);
            PingWatcher.init(API);
        }

        SupportThread.init();
        EmbedCommand.init();
        ThreadsCommand.init();
        SupportCommand.init();
        QuestionCommand.init();
        LogsCommand.init();

        EXECUTOR.scheduleAtFixedRate(ThreadCooldown::cleanupCooldowns, 1L, 1L, TimeUnit.HOURS);

        LOGGER.info("Successfully initialized");
    }

}
