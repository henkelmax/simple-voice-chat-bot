package de.maxhenkel.voicechatbot;

import de.maxhenkel.voicechatbot.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {

    public static DiscordApi API;
    public static Database DB;
    public static ScheduledExecutorService EXECUTOR;

    public static final Logger LOGGER = LogManager.getLogger("voicechatbot");

    public static void main(String[] args) {
        LOGGER.info("Starting bot");

        EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        DB = new Database();
        API = new DiscordApiBuilder().setToken(Environment.TOKEN).setAllIntents().login().join();
        LOGGER.info("Logged in");

        API.updateActivity(ActivityType.WATCHING, "Support");

        Commands.clearCommands();
        Commands.initCommands();

        API.addMessageCreateListener(SupportThread::onMessage);
        API.addButtonClickListener(SupportThread::onButtonClick);
        API.addModalSubmitListener(SupportThread::onModalSubmit);
        API.addSelectMenuChooseListener(SupportThread::onSelectMenuChoose);
        API.addSlashCommandCreateListener(SupportThread::onSlashCommand);

        SupportThread.init();
        LOGGER.info("Successfully initialized");
    }

}
