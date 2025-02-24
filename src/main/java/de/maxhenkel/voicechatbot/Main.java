package de.maxhenkel.voicechatbot;

import de.maxhenkel.voicechatbot.db.Database;
import de.maxhenkel.voicechatbot.portchecker.PortCheckerCommand;
import de.maxhenkel.voicechatbot.support.SupportThread;
import de.maxhenkel.voicechatbot.support.ThreadCooldown;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static JDA API;
    public static Database DB;
    public static ScheduledExecutorService EXECUTOR;

    public static final Logger LOGGER = LogManager.getLogger("voicechatbot");

    public static void main(String[] args) throws InterruptedException, SQLException {
        LOGGER.info("Starting bot");

        if (!Environment.validate()) {
            return;
        }

        EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        DB = new Database();
        API = JDABuilder.create(Environment.TOKEN, Arrays.asList(GatewayIntent.values()))
                .setActivity(Activity.watching("Support"))
                .setAutoReconnect(true)
                .build().awaitReady();
        LOGGER.info("Logged in");

        ButtonRegistry.init();

        API.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                SupportThread.onMessage(event);
                LogUploader.onMessage(event);
                if (Environment.NO_PING_ROLE > 0L) {
                    PingWatcher.onMessage(event);
                }
            }

            @Override
            public void onModalInteraction(@NotNull ModalInteractionEvent event) {
                SupportThread.onModalSubmit(event);
            }

            @Override
            public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
                SupportThread.onSelectMenuChoose(event);
            }

            @Override
            public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
                CommandRegistry.onCommand(event);
            }
        });

        SupportThread.init();
        EmbedCommand.init();
        ThreadsCommand.init();
        SupportCommand.init();
        QuestionCommand.init();
        LogsCommand.init();
        PortCheckerCommand.init();

        CommandRegistry.applyCommands();

        EXECUTOR.scheduleAtFixedRate(ThreadCooldown::cleanupCooldowns, 1L, 1L, TimeUnit.HOURS);

        LOGGER.info("Successfully initialized");
    }

}
