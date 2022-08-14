package de.maxhenkel.voicechatbot;

import de.maxhenkel.voicechatbot.db.Thread;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SupportThread {

    private static final String BUTTON_SUPPORT_KEY = "button_support_key";
    private static final String BUTTON_ABORT_SUPPORT = "button_abort_support";
    private static final String BUTTON_CONFIRM_ANSWERS = "button_confirm_answers";
    private static final String MODAL_SUPPORT_KEY = "modal_support_key";
    private static final String TEXT_FIELD_SUPPORT_KEY = "text_field_support_key";
    private static final String SELECT_MENU_ISSUE = "select_menu_issue";
    private static final String ISSUE_NOT_CONNECTED = "issue_not_connected";
    private static final String ISSUE_MIC_NOT_WORKING = "issue_mic_not_working";
    private static final String ISSUE_CONFIG = "issue_config";
    private static final String ISSUE_OTHER = "issue_other";
    private static final String ISSUE_CRASH = "issue_crash";
    private static final String ISSUE_GENERAL_QUESTION = "issue_general_question";

    public static final List<String> ISSUES = Arrays.asList(ISSUE_NOT_CONNECTED, ISSUE_MIC_NOT_WORKING, ISSUE_CONFIG, ISSUE_OTHER, ISSUE_CRASH, ISSUE_GENERAL_QUESTION);

    public static void init() {
        Channel channel = Main.API.getChannelById(Environment.SUPPORT_CHANNEL_ID).orElse(null);
        if (channel == null) {
            throw new IllegalStateException("Can't find support channel!");
        }

        ServerTextChannel supportChannel = channel.asServerTextChannel().orElse(null);
        if (supportChannel == null) {
            throw new IllegalStateException("Support channel is not a text channel");
        }
    }

    public static void onMessage(MessageCreateEvent event) {
        if (event.getChannel().getId() == Environment.SUPPORT_CHANNEL_ID) {
            ServerTextChannel channel = event.getChannel().asServerTextChannel().orElse(null);
            if (channel == null) {
                return;
            }
            onSupportChannelMessage(channel, event);
            return;
        }
        ServerThreadChannel thread = event.getServerThreadChannel().orElse(null);
        if (thread != null && thread.getParent().getId() == Environment.SUPPORT_CHANNEL_ID) {
            onSupportChannelThreadMessage(thread, event);
            return;
        }
    }

    private static void onSupportChannelThreadMessage(ServerThreadChannel channel, MessageCreateEvent event) {
        MessageAuthor messageAuthor = event.getMessageAuthor();
        if (messageAuthor.isBotUser()) {
            return;
        }
        Thread thread = Main.DB.getThread(channel.getId());

        if (thread == null) {
            event.getMessage().delete();
            return;
        }

        if (thread.isUnlocked()) {
            if (isStaff(messageAuthor)) {
                channel.createUpdater().setAutoArchiveDuration(AutoArchiveDuration.ONE_HOUR).update();
            }
            return;
        }

        if (thread.getUser() != messageAuthor.getId()) {
            event.getMessage().delete();
            return;
        }
        event.getMessage().reply("<@%s>, please follow the instructions of the bot to be able to write messages in this thread!".formatted(messageAuthor.getId())).thenAccept(message -> {
            Main.EXECUTOR.schedule(() -> {
                event.getMessage().delete();
            }, 3, TimeUnit.SECONDS);
            Main.EXECUTOR.schedule(() -> {
                message.delete();
            }, 10, TimeUnit.SECONDS);
        });

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

    private static void onSupportChannelMessage(ServerTextChannel channel, MessageCreateEvent event) {
        MessageAuthor messageAuthor = event.getMessageAuthor();
        if (messageAuthor.isBotUser()) {
            return;
        }
        Thread thread = getThread(messageAuthor.getId());
        if (thread != null) {
            event.getMessage().reply("Hey <@%s>, you already have a support thread: <#%s>".formatted(event.getMessageAuthor().getId(), thread.getThread())).thenAccept(message -> {
                Main.EXECUTOR.schedule(() -> {
                    event.getMessage().delete();
                    message.delete();
                }, 10, TimeUnit.SECONDS);
            });
            return;
        }

        event.getMessage()
                .createThread("Support thread for %s".formatted(messageAuthor.getDisplayName()), AutoArchiveDuration.ONE_HOUR)
                .thenAccept(t -> onThreadCreated(t, messageAuthor));
    }

    @Nullable
    private static Thread getThread(long user) {
        Thread thread = Main.DB.getThreadByUser(user);

        if (thread == null) {
            return null;
        }

        Channel c = Main.API.getChannelById(thread.getThread()).orElse(null);
        if (c == null) {
            Main.DB.removeThread(thread.getThread());
            return null;
        }

        ServerThreadChannel threadChannel = c.asServerThreadChannel().orElse(null);

        if (threadChannel == null) {
            Main.DB.removeThread(thread.getThread());
            return null;
        }

        if (threadChannel.isLocked()) {
            Main.DB.removeThread(thread.getThread());
            return null;
        }
        return thread;
    }

    private static void onThreadCreated(ServerThreadChannel thread, MessageAuthor messageAuthor) {
        thread.addThreadMember(messageAuthor.getId());
        Main.DB.addThread(new Thread(messageAuthor.getId(), thread.getId()));

        thread.sendMessage(
                new EmbedBuilder()
                        .setTitle("Support")
                        .setDescription("""
                                **To get support you must first read the [wiki](https://modrepo.de/minecraft/voicechat/wiki).**
                                Please make sure that you have read everything thoroughly and that your problem is certainly not covered there.
                                If this is the case, please generate a support key on the right hand panel in the wiki.
                                After clicking the `Get Support!` button below this message you will be asked to enter the support key.
                                """)
                        .addField("Useful Links",
                                String.join(" | ",
                                        "[Mod Description](https://modrepo.de/minecraft/voicechat)",
                                        "[FAQ](https://modrepo.de/minecraft/voicechat/faq)",
                                        "[Wiki](https://modrepo.de/minecraft/voicechat/wiki)",
                                        "[Mod Downloads](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat/files/all)",
                                        "[Plugin Downloads](https://www.curseforge.com/minecraft/bukkit-plugins/simple-voice-chat/files/all)"
                                )
                        )
                        .setColor(Color.BLUE),
                ActionRow.of(
                        new ButtonBuilder().setCustomId(BUTTON_SUPPORT_KEY).setLabel("Get Support!").build(),
                        new ButtonBuilder().setCustomId(BUTTON_ABORT_SUPPORT).setLabel("Nevermind...").build()
                )
        );
    }

    public static void onButtonClick(ButtonClickEvent event) {
        ServerThreadChannel thread = getThread(event.getInteraction());
        if (thread == null || thread.getParent().getId() != Environment.SUPPORT_CHANNEL_ID) {
            return;
        }
        Thread t = getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().createImmediateResponder().respond();
            return;
        }
        // Only the creator of the thread can interact with buttons

        String id = event.getButtonInteraction().getCustomId();
        if (BUTTON_ABORT_SUPPORT.equals(id)) {
            closeThread(thread, event.getButtonInteraction().getUser().getId());
            event.getButtonInteraction().createImmediateResponder().respond();
        } else if (BUTTON_SUPPORT_KEY.equals(id)) {
            ButtonInteraction buttonInteraction = event.getButtonInteraction();
            buttonInteraction.respondWithModal(MODAL_SUPPORT_KEY, "Support Key",
                    ActionRow.of(TextInput.create(TextInputStyle.SHORT, TEXT_FIELD_SUPPORT_KEY, "Your Support Key"))
            );
        } else if (BUTTON_CONFIRM_ANSWERS.equals(id)) {
            event.getButtonInteraction().createOriginalMessageUpdater().removeAllComponents().update();
            onConfirmAnswers(thread, t, event);
        }
    }

    private static void closeThread(ServerThreadChannel thread, long locker) {
        thread.sendMessage(new EmbedBuilder().setDescription("<@%s> locked this thread.".formatted(locker)).setColor(Color.RED)).thenAccept(message -> {
            Main.DB.removeThread(thread.getId());
            thread.createUpdater().setArchivedFlag(true).setLockedFlag(true).setAutoArchiveDuration(AutoArchiveDuration.ONE_HOUR).update();
        });
    }

    private static void onConfirmAnswers(ServerThreadChannel thread, Thread t, ButtonClickEvent event) {
        thread.getMessages(10).thenAccept(messages -> {
            if (messages.stream().noneMatch(message -> message.getAuthor().getId() == t.getUser())) {
                thread.sendMessage(
                        new EmbedBuilder()
                                .setTitle("No messages")
                                .setDescription("""
                                        No messages have been detected.
                                        """)
                                .setColor(Color.RED)
                );
                event.getButtonInteraction().getMessage().toMessageBuilder().send(thread);
                return;
            }

            thread.sendMessage(
                    new EmbedBuilder()
                            .setTitle("Success")
                            .setDescription("""
                                    Staff will now be notified.
                                     
                                    Please note that timezones exist and people might not be available instantly.
                                    """)
                            .setColor(Color.GREEN),
                    ActionRow.of(noHelpButton())
            );
            thread.createUpdater().setAutoArchiveDuration(AutoArchiveDuration.ONE_DAY).update();
            addStaff(thread, t);
        });
    }

    public static void onModalSubmit(ModalSubmitEvent event) {
        ServerThreadChannel thread = getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().createImmediateResponder().respond();
            return;
        }

        String id = event.getModalInteraction().getCustomId();
        if (MODAL_SUPPORT_KEY.equals(id)) {
            String value = event.getModalInteraction().getTextInputValueByCustomId(TEXT_FIELD_SUPPORT_KEY).orElse(null);
            onSupportKeyProvided(thread, event.getModalInteraction(), value);
        }
    }

    private static void onSupportKeyProvided(ServerThreadChannel thread, ModalInteraction modalInteraction, String supportKey) {
        modalInteraction.createImmediateResponder().respond();
        long userId = modalInteraction.getUser().getId();
        if (!verifySupportKey(supportKey)) {
            thread.sendMessage(new EmbedBuilder().setDescription("<@%s> provided an invalid support key: `%s`.".formatted(userId, supportKey)).setColor(Color.RED), ActionRow.of(
                    new ButtonBuilder().setCustomId(BUTTON_SUPPORT_KEY).setLabel("Let me try again").build()
            ));
            return;
        }
        thread.sendMessage(new EmbedBuilder().setDescription("The support key of <@%s> is `%s`.".formatted(userId, supportKey)).setColor(Color.GREEN));

        clearAllComponents(thread).thenAccept(messages -> {
            sendSupportTemplateMessage(thread);
        });
    }

    private static CompletableFuture<List<Message>> clearAllComponents(ServerThreadChannel thread) {
        List<CompletableFuture<Message>> futures = new ArrayList<>();
        thread.getMessagesAsStream().forEach(message -> {
            if (message.getType().equals(MessageType.UNKNOWN)) {
                return;
            }
            if (message.getAuthor().getId() == Main.API.getClientId()) {
                CompletableFuture<Message> future = message.createUpdater().removeAllComponents().applyChanges();
                futures.add(future);
            }
        });

        return all(futures);
    }

    private static <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(ignored -> Collections.emptyList());
    }

    private static void sendSupportTemplateMessage(ServerThreadChannel thread) {
        thread.sendMessage(
                new EmbedBuilder()
                        .setTitle("Select your issue")
                        .setDescription("""
                                Please select your issue from the menu below.
                                                                
                                Alternatively you can take a look at <#%s>.
                                """.formatted(Environment.COMMON_ISSUES_CHANNEL_ID))
                        .setColor(Color.BLUE),
                ActionRow.of(
                        new SelectMenuBuilder().setCustomId(SELECT_MENU_ISSUE)
                                .addOption(new SelectMenuOptionBuilder().setLabel(translateIssue(ISSUE_NOT_CONNECTED)).setValue(ISSUE_NOT_CONNECTED).build())
                                .addOption(new SelectMenuOptionBuilder().setLabel(translateIssue(ISSUE_MIC_NOT_WORKING)).setValue(ISSUE_MIC_NOT_WORKING).build())
                                .addOption(new SelectMenuOptionBuilder().setLabel(translateIssue(ISSUE_CONFIG)).setValue(ISSUE_CONFIG).build())
                                .addOption(new SelectMenuOptionBuilder().setLabel(translateIssue(ISSUE_OTHER)).setValue(ISSUE_OTHER).build())
                                .addOption(new SelectMenuOptionBuilder().setLabel(translateIssue(ISSUE_CRASH)).setValue(ISSUE_CRASH).build())
                                .addOption(new SelectMenuOptionBuilder().setLabel(translateIssue(ISSUE_GENERAL_QUESTION)).setValue(ISSUE_GENERAL_QUESTION).build())
                                .setMinimumValues(1)
                                .setMaximumValues(1)
                                .setPlaceholder("Select issue")
                                .build()
                ),
                ActionRow.of(
                        noHelpButton()
                )
        );
    }

    public static void onSelectMenuChoose(SelectMenuChooseEvent event) {
        ServerThreadChannel thread = getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().createImmediateResponder().respond();
            return;
        }

        String customId = event.getSelectMenuInteraction().getCustomId();

        if (!SELECT_MENU_ISSUE.equals(customId)) {
            return;
        }
        List<SelectMenuOption> chosenOptions = event.getSelectMenuInteraction().getChosenOptions();

        if (chosenOptions.size() <= 0) {
            return;
        }
        String selection = chosenOptions.get(0).getValue();

        clearAllComponents(thread).thenAccept(messages -> {
            thread.sendMessage(new EmbedBuilder()
                    .setTitle("`%s` selected.".formatted(translateIssue(selection)))
                    .setDescription("""
                            Please provide additional information, so we can help you.
                            Once you did, you will get help by our team.
                            """)
                    .setColor(Color.GREEN)
            );
            sendSupportQuestions(thread, selection);
        });
    }

    public static void sendSupportQuestions(ServerThreadChannel thread, String selection) {
        if (ISSUE_NOT_CONNECTED.equals(selection)) {
            thread.sendMessage(new EmbedBuilder()
                    .setTitle("Disclaimer")
                    .setDescription("""
                            If you are hosting your server with a Minecraft hosting provider, please do the following:
                                                        
                            ⦁ Go to <#%s> and look if a guide for your hoster exists
                            ⦁ If there is no guide for your hoster, please **contact the support of your hoster**
                            ⦁ If you found a guide for your hoster in <#%s>, but it doesn't work, please also contact your hoster first
                                                        
                            **We can't help you with the configuration for specific Minecraft hosters! Please always contact their support first!**
                            """.formatted(Environment.SERVER_HOSTING_CHANNEL_ID, Environment.SERVER_HOSTING_CHANNEL_ID))
                    .setColor(Color.RED)
            );
        } else if (ISSUE_MIC_NOT_WORKING.equals(selection)) {
            thread.sendMessage(new EmbedBuilder()
                    .setTitle("Disclaimer")
                    .setDescription("""
                            If you are on **MacOS**, you need to patch your launcher in order to get your microphone working!
                                                        
                            ⦁ If you don't know how to patch your launcher, read [this](https://github.com/henkelmax/simple-voice-chat/tree/1.19.2/macos)
                            ⦁ If there is no patcher popping up when launching your game, download the [standalone patcher](https://github.com/henkelmax/simple-voice-chat/tree/1.19.2/macos#standalone-version)
                                                        
                            """)
                    .setColor(Color.RED)
            );
        } else if (ISSUE_CONFIG.equals(selection)) {
            thread.sendMessage(new EmbedBuilder()
                    .setTitle("Disclaimer")
                    .setDescription("""
                            While editing configuration files, make sure the client/server is stopped.
                            If the config values keep resetting, this is most likely the problem.
                            If you can't find the config files, make sure the client/server was started at least once, so that the files are generated.
                            """)
                    .addField("Fabric/Quilt config location", "*Server*:\n`config/voicechat/voicechat-server.properties`\n*Client*:\n`config/voicechat/voicechat-client.properties`")
                    .addField("Forge config location", "*Server*:\n`<Your world folder>/serverconfig/voicechat-server.toml`\n*Client*:\n`config/voicechat-client.toml`")
                    .addField("Bukkit/Spigot/Paper config location", "`plugins/voicechat/voicechat-server.properties`")
                    .setColor(Color.RED)
            );
        } else if (ISSUE_CRASH.equals(selection)) {
            thread.sendMessage(new EmbedBuilder()
                    .setTitle("Disclaimer")
                    .setDescription("""
                            If you encountered a crash, please provide log files of both your client and server!
                                                        
                            Please send both logs as a file in this channel.
                            """)
                    .addField("Client logs", "`.minecraft/logs/latest.log`")
                    .addField("Server logs", "`logs/latest.log`")
                    .setColor(Color.RED)
            );
        }

        thread.sendMessage(new EmbedBuilder()
                        .setTitle("Please answer the following questions")
                        .setDescription("""
                                You can just send the answers as normal text messages in this thread.
                                                                    
                                %s
                                                                    
                                Once you answered every question, confirm them by pressing the `Confirm` button.
                                """.formatted(getQuestions(selection).stream().map("⦁ %s"::formatted).collect(Collectors.joining("\n"))))
                        .setColor(Color.BLUE),
                ActionRow.of(
                        noHelpButton(),
                        new ButtonBuilder().setCustomId(BUTTON_CONFIRM_ANSWERS).setLabel("Confirm").build()
                )
        ).thenAccept(message -> {
            Main.DB.unlockThread(thread.getId());
        });
    }

    private static Button noHelpButton() {
        return new ButtonBuilder().setCustomId(BUTTON_ABORT_SUPPORT).setLabel("I don't need help anymore").build();
    }

    public static List<String> getQuestions(String id) {
        List<String> questions = new ArrayList<>();
        if (ISSUE_GENERAL_QUESTION.equals(id)) {
            questions.add("What is your question?");
        } else {
            questions.add("What is your issue?");
        }

        questions.add("What Minecraft version are you using?");
        questions.add("What voice chat mod version are you using on your game? *(Please post the full filename of the mod jar)*");
        questions.add("What voice chat mod/plugin version are you using on your server? *(Please post the full filename of the mod/plugin jar)*");
        if (ISSUE_NOT_CONNECTED.equals(id)) {
            questions.add("What server software are you using? *(Fabric/Forge/Bukkit/Spigot/Paper etc)*");
            questions.add("Are you using a proxy server? *(Bungeecord/Waterfall/Velocity etc)*");
            questions.add("Are you using any DDoS protection? *(TCPShield etc)*");
            questions.add("Where are you hosting your server? *(Bloom/Aternos/Own PC/VPS etc)*");
        } else if (ISSUE_MIC_NOT_WORKING.equals(id)) {
            questions.add("Operating system are you using? *(Windows/MacOS/Linux)*");
            questions.add("Operating system version are you on? (MacOS 10.15/Windows 10 etc)");
        } else if (ISSUE_CRASH.equals(id)) {
            questions.add("Did the crash occur on the client or the server?");
        }

        return questions;
    }

    public static String translateIssue(String issue) {
        return switch (issue) {
            case ISSUE_NOT_CONNECTED -> "Voice chat not connected";
            case ISSUE_MIC_NOT_WORKING -> "Microphone not working";
            case ISSUE_CONFIG -> "Config file issues";
            case ISSUE_OTHER -> "Other issue";
            case ISSUE_CRASH -> "Crash";
            case ISSUE_GENERAL_QUESTION -> "General question";
            default -> "N/A";
        };
    }

    private static void addStaff(ServerThreadChannel thread, Thread t) {
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

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("New Support Request")
                .addField("User", "<@%s>".formatted(t.getUser()))
                .addField("Thread", "<#%s>".formatted(thread.getId()))
                .setTimestampToNow()
                .setColor(Color.BLUE)
        ).thenAccept(message -> {
            message.addReaction("✅");
        });
    }

    private static ServerThreadChannel getThread(InteractionBase interactionBase) {
        TextChannel channel = interactionBase.getChannel().orElse(null);
        if (channel == null) {
            return null;
        }
        ServerThreadChannel thread = channel.asServerThreadChannel().orElse(null);
        if (thread == null) {
            return null;
        }
        return thread;
    }

    private static Thread getThreadIfOwner(InteractionBase interactionBase) {
        Thread t = Main.DB.getThreadByUser(interactionBase.getUser().getId());
        TextChannel channel = interactionBase.getChannel().orElse(null);
        if (channel == null || t == null || t.getThread() != channel.getId()) {
            return null;
        }
        return t;
    }

    private static final Pattern SUPPORT_KEY_PATTERN = Pattern.compile("^S-(?<numbers>[1-9]+)$");

    private static boolean verifySupportKey(String supportKey) {
        if (supportKey == null) {
            return false;
        }

        Matcher matcher = SUPPORT_KEY_PATTERN.matcher(supportKey);
        if (!matcher.matches()) {
            return false;
        }

        String numbers = matcher.group("numbers");

        return Arrays.stream(numbers.split("")).map(Integer::parseInt).reduce(0, Integer::sum) == 69; // Nice!
    }

    public static void onSlashCommand(SlashCommandCreateEvent event) {
        @Nullable ServerThreadChannel thread = getThread(event.getInteraction());

        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        String commandName = interaction.getCommandName();
        if (Commands.CLOSE_COMMAND.equals(commandName)) {
            if (thread != null) {
                if (isStaff(interaction.getUser(), interaction.getServer().orElse(null))) {
                    closeThread(thread, interaction.getUser().getId());
                    interaction.createImmediateResponder()
                            .setContent("Thread closed")
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond();
                }
            }
        } else if (Commands.UNLOCK_COMMAND.equals(commandName)) {
            if (thread != null) {
                if (isStaff(interaction.getUser(), interaction.getServer().orElse(null))) {
                    Main.DB.unlockThread(thread.getId());
                    interaction.createImmediateResponder()
                            .setContent("Thread unlocked")
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond();
                }
            }
        } else if (Commands.ISSUE_COMMAND.equals(commandName)) {
            if (thread != null) {
                if (isStaff(interaction.getUser(), interaction.getServer().orElse(null))) {
                    List<SlashCommandInteractionOption> arguments = event.getSlashCommandInteraction().getArguments();
                    if (arguments.size() != 1) {
                        return;
                    }
                    String value = arguments.get(0).getStringValue().orElse(null);
                    if (value == null) {
                        return;
                    }
                    thread.sendMessage(new EmbedBuilder()
                            .setTitle("Issue type changed")
                            .setDescription("""
                                    <@%s> changed the issue type to `%s`.
                                                                        
                                    Please provide additional information, so we can help you.
                                    """.formatted(event.getInteraction().getUser().getId(), translateIssue(value)))
                            .setColor(Color.GREEN)
                    );
                    sendSupportQuestions(thread, value);
                    interaction.createImmediateResponder()
                            .setContent(translateIssue(value))
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond();
                }
            }
        }
    }
}
