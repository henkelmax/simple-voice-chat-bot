package de.maxhenkel.voicechatbot.support;

import de.maxhenkel.voicechatbot.*;
import de.maxhenkel.voicechatbot.db.Thread;
import de.maxhenkel.voicechatbot.support.issues.Issue;
import de.maxhenkel.voicechatbot.support.issues.Issues;
import org.javacord.api.entity.channel.AutoArchiveDuration;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.entity.message.component.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SupportThread {

    public static final String BUTTON_SUPPORT_KEY = "button_support_key";
    public static final String BUTTON_ABORT_SUPPORT = "button_abort_support";
    public static final String BUTTON_CONFIRM_ANSWERS = "button_confirm_answers";

    public static final String MODAL_SUPPORT_KEY = "modal_support_key";

    public static final String TEXT_FIELD_SUPPORT_KEY = "text_field_support_key";

    public static final String SELECT_MENU_ISSUE = "select_menu_issue";

    public static final String CLOSE_COMMAND = "close";
    public static final String UNLOCK_COMMAND = "unlock";
    public static final String ISSUE_COMMAND = "issue";
    public static final String CLEANUP_COMMAND = "cleanup";

    public static void init() {
        Channel channel = Main.API.getChannelById(Environment.SUPPORT_CHANNEL_ID).orElse(null);
        if (channel == null) {
            throw new IllegalStateException("Can't find support channel!");
        }

        ServerTextChannel supportChannel = channel.asServerTextChannel().orElse(null);
        if (supportChannel == null) {
            throw new IllegalStateException("Support channel is not a text channel");
        }

        ButtonRegistry.registerButton(BUTTON_SUPPORT_KEY, SupportThread::onSupportKeyButtonPressed);
        ButtonRegistry.registerButton(BUTTON_ABORT_SUPPORT, SupportThread::onAbortSupportButtonPressed);
        ButtonRegistry.registerButton(BUTTON_CONFIRM_ANSWERS, SupportThread::onConfirmAnswersButtonPressed);
        CommandRegistry.registerCommand(CLOSE_COMMAND, "Closes a support thread", SupportThread::onCloseCommand);
        CommandRegistry.registerCommand(UNLOCK_COMMAND, "Unlocks a thread", SupportThread::onUnlockCommand);
        CommandRegistry.registerCommand(ISSUE_COMMAND, "Sends an issue template to a thread",
                Collections.singletonList(SlashCommandOption.createWithChoices(
                        SlashCommandOptionType.STRING,
                        "issue",
                        "The issue to show",
                        true,
                        Issues.ISSUES.stream().map(issue -> SlashCommandOptionChoice.create(issue.getName(), issue.getId())).collect(Collectors.toList())
                )),
                SupportThread::onIssueCommand);
        CommandRegistry.registerCommand(CLEANUP_COMMAND, "Cleans up old threads", SupportThread::onCleanupCommand, PermissionType.ADMINISTRATOR);
    }

    private static void onCleanupCommand(SlashCommandCreateEvent event) {
        event.getSlashCommandInteraction().respondLater().thenAccept(responseUpdater -> {
            responseUpdater.setContent("Archiving all threads older than a week...").update();
            AtomicInteger removed = new AtomicInteger();
            Main.DB.getThreads(t -> {
                ServerThreadChannel thread = Main.API.getServerThreadChannelById(t.getThread()).orElse(null);
                if (thread == null) {
                    Main.DB.removeThread(t.getThread());
                    responseUpdater.setContent("Archived %s threads...".formatted(removed.incrementAndGet())).update();
                    return;
                }
                if (thread.getArchiveTimestamp().isBefore(Instant.now().minus(7, ChronoUnit.SECONDS))) {
                    SupportThreadUtils.closeThread(thread, t, Main.API.getClientId());
                    Main.DB.removeThread(t.getThread());
                    responseUpdater.setContent("Archived %s threads...".formatted(removed.incrementAndGet())).update();
                }
            });
            responseUpdater.setContent("Finished archiving %s threads.".formatted(removed.get())).update();
        });
    }

    private static void onIssueCommand(SlashCommandCreateEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        if (thread == null) {
            return;
        }
        if (!SupportThreadUtils.isStaff(interaction.getUser(), interaction.getServer().orElse(null))) {
            return;
        }
        List<SlashCommandInteractionOption> arguments = event.getSlashCommandInteraction().getArguments();
        if (arguments.size() != 1) {
            return;
        }
        String value = arguments.get(0).getStringValue().orElse(null);
        if (value == null) {
            return;
        }
        Issue issue = Issues.byId(value);
        if (issue == null) {
            return;
        }
        thread.sendMessage(new EmbedBuilder()
                .setTitle("Issue type changed")
                .setDescription("""
                        <@%s> changed the issue type to `%s`.
                                                            
                        Please provide additional information, so we can help you.
                        """.formatted(event.getInteraction().getUser().getId(), issue.getName()))
                .setColor(Color.GREEN)
        );
        issue.onSelectIssue(thread);
        issue.sendQuestions(thread);
        interaction.createImmediateResponder()
                .setContent(issue.getName())
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

    private static void onUnlockCommand(SlashCommandCreateEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        if (thread == null) {
            return;
        }
        if (!SupportThreadUtils.isStaff(interaction.getUser(), interaction.getServer().orElse(null))) {
            return;
        }

        Main.DB.unlockThread(thread.getId());
        SupportThreadUtils.notifyStaff(thread).thenAccept(unused -> {
            Thread t = Main.DB.getThread(thread.getId());
            if (t != null) {
                SupportThreadUtils.updateStaffNotification(t, "<@%s> unlocked the thread".formatted(event.getInteraction().getUser().getId()));
            }
        });
        interaction.createImmediateResponder()
                .setContent("Thread unlocked")
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();

        //TODO send embed that staff joined
    }

    private static void onCloseCommand(SlashCommandCreateEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        if (thread == null) {
            interaction.createImmediateResponder().respond();
            return;
        }
        if (!SupportThreadUtils.isStaff(interaction.getUser(), interaction.getServer().orElse(null))) {
            return;
        }
        Thread t = Main.DB.getThread(thread.getId());
        if (t != null) {
            SupportThreadUtils.closeThread(thread, t, interaction.getUser().getId());
            interaction.createImmediateResponder()
                    .setContent("Thread closed")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        } else {
            interaction.createImmediateResponder()
                    .setContent("Can't find thread")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
        }
    }

    private static void onConfirmAnswersButtonPressed(ButtonClickEvent event) {
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (t == null || thread == null) {
            event.getInteraction().createImmediateResponder().respond();
            return;
        }
        event.getButtonInteraction().createOriginalMessageUpdater().removeAllComponents().update();
        onConfirmAnswers(thread, t, event);
    }

    private static void onAbortSupportButtonPressed(ButtonClickEvent event) {
        SupportThreadUtils.closeThread(SupportThreadUtils.getThread(event.getInteraction()), SupportThreadUtils.getThreadIfOwner(event.getInteraction()), event.getInteraction().getUser().getId());
        event.getButtonInteraction().createImmediateResponder().respond();
    }

    private static void onSupportKeyButtonPressed(ButtonClickEvent event) {
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().createImmediateResponder().respond();
            return;
        }

        ButtonInteraction buttonInteraction = event.getButtonInteraction();
        buttonInteraction.respondWithModal(MODAL_SUPPORT_KEY, "Support Key",
                ActionRow.of(TextInput.create(TextInputStyle.SHORT, TEXT_FIELD_SUPPORT_KEY, "Your Support Key"))
        );
    }

    public static void onMessage(MessageCreateEvent event) {
        if (SupportThreadUtils.isSupportChannel(event.getChannel())) {
            onSupportChannelMessage(event);
            return;
        }

        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getChannel());
        if (thread != null) {
            onSupportChannelThreadMessage(thread, event);
            return;
        }
    }

    private static void onSupportChannelMessage(MessageCreateEvent event) {
        MessageAuthor messageAuthor = event.getMessageAuthor();
        if (!messageAuthor.isRegularUser()) {
            return;
        }
        Thread thread = SupportThreadUtils.getThread(messageAuthor.getId());
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
            if (SupportThreadUtils.isStaff(messageAuthor)) {
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
                        new ButtonBuilder().setCustomId(BUTTON_SUPPORT_KEY).setLabel("Get Support!").setStyle(ButtonStyle.PRIMARY).build(),
                        new ButtonBuilder().setCustomId(BUTTON_ABORT_SUPPORT).setLabel("Nevermind...").setStyle(ButtonStyle.DANGER).build()
                )
        );
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
                    ActionRow.of(SupportThreadUtils.closeThreadButton())
            );
            thread.createUpdater().setAutoArchiveDuration(AutoArchiveDuration.ONE_DAY).update();
            SupportThreadUtils.notifyStaff(thread, t);
        });
    }

    public static void onModalSubmit(ModalSubmitEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
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
        if (!SupportKey.verifySupportKey(supportKey)) {
            thread.sendMessage(new EmbedBuilder().setDescription("<@%s> provided an invalid support key: `%s`.".formatted(userId, supportKey)).setColor(Color.RED), ActionRow.of(
                    new ButtonBuilder().setCustomId(BUTTON_SUPPORT_KEY).setLabel("Let me try again").setStyle(ButtonStyle.PRIMARY).build()
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
        SelectMenuBuilder selectMenuBuilder = new SelectMenuBuilder()
                .setCustomId(SELECT_MENU_ISSUE)
                .setMinimumValues(1)
                .setMaximumValues(1)
                .setPlaceholder("Select issue");

        for (Issue issue : Issues.ISSUES) {
            selectMenuBuilder.addOption(new SelectMenuOptionBuilder().setLabel(issue.getName()).setValue(issue.getId()).build());
        }

        thread.sendMessage(
                new EmbedBuilder()
                        .setTitle("Select your issue")
                        .setDescription("""
                                Please select your issue from the menu below.
                                                                
                                Alternatively you can take a look at <#%s>.
                                """.formatted(Environment.COMMON_ISSUES_CHANNEL_ID))
                        .setColor(Color.BLUE),
                ActionRow.of(selectMenuBuilder.build()),
                ActionRow.of(SupportThreadUtils.closeThreadButton())
        );
    }

    public static void onSelectMenuChoose(SelectMenuChooseEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
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
            Issue issue = Issues.byId(selection);
            if (issue == null) {
                return;
            }
            thread.sendMessage(new EmbedBuilder()
                    .setTitle("`%s` selected.".formatted(issue.getName()))
                    .setDescription("""
                            Please provide additional information, so we can help you.
                            Once you did, you will get help by our team.
                            """)
                    .setColor(Color.GREEN)

            );
            issue.onSelectIssue(thread);
            issue.sendQuestions(thread);
        });
    }

}
