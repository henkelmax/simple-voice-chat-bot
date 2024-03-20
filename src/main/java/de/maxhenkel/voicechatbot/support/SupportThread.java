package de.maxhenkel.voicechatbot.support;

import de.maxhenkel.voicechatbot.*;
import de.maxhenkel.voicechatbot.db.Thread;
import de.maxhenkel.voicechatbot.support.issues.Issue;
import de.maxhenkel.voicechatbot.support.issues.Issues;
import org.javacord.api.entity.channel.AutoArchiveDuration;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.entity.message.component.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

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

    public static final String BUTTON_GET_SUPPORT = "button_get_support";
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
        ServerTextChannel supportChannel = SupportThreadUtils.getChannel(Environment.SUPPORT_CHANNEL_ID);
        if (supportChannel == null) {
            throw new IllegalStateException("Support channel not found");
        }

        ButtonRegistry.registerButton(BUTTON_GET_SUPPORT, SupportThread::onGetSupportButtonPressed);
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

        supportChannel.getMessagesAsStream().forEach(message -> {
            if (message.getAuthor().getId() == Main.API.getClientId()) {
                message.delete().exceptionally(new ExceptionHandler<>());
            }
        });

        supportChannel.sendMessage(new EmbedBuilder()
                        .setTitle("Support")
                        .setDescription("""
                                Please press the `Get Support` button to get support.
                                You will get asked a couple of questions about your issue and then you'll get help by our staff.
                                """)
                        .setColor(Color.GREEN),
                ActionRow.of(new ButtonBuilder().setCustomId(SupportThread.BUTTON_GET_SUPPORT).setLabel("Get Support").setStyle(ButtonStyle.SUCCESS).build())
        ).exceptionally(new ExceptionHandler<>());
    }

    private static void onGetSupportButtonPressed(ButtonClickEvent event) {
        ServerTextChannel supportChannel = SupportThreadUtils.getChannel(Environment.SUPPORT_THREAD_CHANNEL_ID);
        if (supportChannel == null) {
            Main.LOGGER.error("Support thread channel not found");
            return;
        }
        User user = event.getInteraction().getUser();
        Thread t = SupportThreadUtils.getThread(user.getId());
        if (t != null) {
            event.getButtonInteraction().createImmediateResponder().setContent("Hey <@%s>, you already have a support thread: <#%s>".formatted(user.getId(), t.getThread())).setFlags(MessageFlag.EPHEMERAL).respond().exceptionally(new ExceptionHandler<>());
            return;
        }
        if (ThreadCooldown.isOnCooldown(user.getId())) {
            event.getButtonInteraction().createImmediateResponder().setContent("Hey <@%s>, you already recently opened a support thread. Please wait a while to create a new one.".formatted(user.getId())).setFlags(MessageFlag.EPHEMERAL).respond().exceptionally(new ExceptionHandler<>());
            return;
        }
        supportChannel.sendMessage("Support for <@%s>".formatted(user.getId())).thenAccept(message -> {
            message.createThread("Support thread for %s".formatted(user.getName()), AutoArchiveDuration.ONE_HOUR).thenAccept(thread -> {
                event.getButtonInteraction().createImmediateResponder().setContent("Hey <@%s>, please follow the steps in your support thread: <#%s>".formatted(user.getId(), thread.getId())).setFlags(MessageFlag.EPHEMERAL).respond().exceptionally(new ExceptionHandler<>());
                thread.addThreadMember(user.getId()).thenAccept(unused -> {
                    Main.DB.addThread(new Thread(user.getId(), thread.getId()));
                    ThreadCooldown.setCooldown(user.getId());
                    onThreadCreated(thread, user);
                }).exceptionally(new ExceptionHandler<>());
            }).exceptionally(new ExceptionHandler<>());
        }).exceptionally(new ExceptionHandler<>());
    }

    private static void onCleanupCommand(SlashCommandCreateEvent event) {
        event.getSlashCommandInteraction().respondLater().thenAccept(responseUpdater -> {
            responseUpdater.setContent("Archiving all stale threads...").update().exceptionally(new ExceptionHandler<>());
            AtomicInteger removed = new AtomicInteger();
            Main.DB.getThreads(t -> {
                ServerThreadChannel thread = Main.API.getServerThreadChannelById(t.getThread()).orElse(null);
                if (thread == null) {
                    Main.DB.removeThread(t.getThread());
                    responseUpdater.setContent("Archived %s threads...".formatted(removed.incrementAndGet())).update().exceptionally(new ExceptionHandler<>());
                    return;
                }
                thread.getMessages(1).thenAccept(messages -> {
                    if (messages.isEmpty() || messages.first().getCreationTimestamp().isBefore(Instant.now().minus(Environment.SUPPORT_STALE_DAYS, ChronoUnit.DAYS))) {
                        SupportThreadUtils.closeThread(thread, t, Main.API.getClientId());
                        Main.DB.removeThread(t.getThread());
                        responseUpdater.setContent("Archived %s threads...".formatted(removed.incrementAndGet())).update().exceptionally(new ExceptionHandler<>());
                    }
                }).exceptionally(new ExceptionHandler<>());
            });
            Main.EXECUTOR.schedule(() -> {
                responseUpdater.setContent("Finished archiving %s threads.".formatted(removed.get())).update().exceptionally(new ExceptionHandler<>());
            }, 5, TimeUnit.SECONDS);
        }).exceptionally(new ExceptionHandler<>());
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
        thread.addThreadMember(event.getInteraction().getUser());
        thread.sendMessage(new EmbedBuilder()
                .setTitle("Issue type changed")
                .setDescription("""
                        <@%s> changed the issue type to `%s`.
                                                            
                        Please provide additional information, so we can help you.
                        """.formatted(event.getInteraction().getUser().getId(), issue.getName()))
                .setColor(Color.GREEN)
        ).exceptionally(new ExceptionHandler<>());
        issue.onSelectIssue(thread);
        issue.sendQuestions(thread);
        interaction.createImmediateResponder()
                .setContent(issue.getName())
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().exceptionally(new ExceptionHandler<>());
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
        }).exceptionally(new ExceptionHandler<>());
        interaction.createImmediateResponder()
                .setContent("Thread unlocked")
                .setFlags(MessageFlag.EPHEMERAL)
                .respond().exceptionally(new ExceptionHandler<>());
    }

    private static void onCloseCommand(SlashCommandCreateEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        if (thread == null) {
            interaction.respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
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
                    .respond()
                    .exceptionally(new ExceptionHandler<>());
        } else {
            interaction.createImmediateResponder()
                    .setContent("Can't find thread")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond()
                    .exceptionally(new ExceptionHandler<>());
        }
    }

    private static void onConfirmAnswersButtonPressed(ButtonClickEvent event) {
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (t == null || thread == null) {
            event.getInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
            return;
        }
        onConfirmAnswers(thread, t, event);
    }

    private static void onAbortSupportButtonPressed(ButtonClickEvent event) {
        SupportThreadUtils.closeThread(SupportThreadUtils.getThread(event.getInteraction()), SupportThreadUtils.getThreadIfOwner(event.getInteraction()), event.getInteraction().getUser().getId());
        event.getButtonInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
    }

    private static void onSupportKeyButtonPressed(ButtonClickEvent event) {
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
            return;
        }

        ButtonInteraction buttonInteraction = event.getButtonInteraction();
        buttonInteraction.respondWithModal(MODAL_SUPPORT_KEY, "Support Key",
                ActionRow.of(TextInput.create(TextInputStyle.SHORT, TEXT_FIELD_SUPPORT_KEY, "Your Support Key"))
        ).exceptionally(new ExceptionHandler<>());
    }

    public static void onMessage(MessageCreateEvent event) {
        ServerThreadChannel channel = SupportThreadUtils.getThread(event.getChannel());
        if (channel == null) {
            return;
        }

        MessageAuthor messageAuthor = event.getMessageAuthor();
        if (!messageAuthor.isRegularUser()) {
            return;
        }
        Thread thread = Main.DB.getThread(channel.getId());

        if (thread == null) {
            event.getMessage().delete().thenAccept(unused -> {
                channel.createUpdater().setArchivedFlag(true).setLockedFlag(true).setAutoArchiveDuration(AutoArchiveDuration.ONE_HOUR).update().exceptionally(new ExceptionHandler<>());
            }).exceptionally(new ExceptionHandler<>());
            return;
        }

        if (thread.isUnlocked()) {
            if (SupportThreadUtils.isStaff(messageAuthor)) {
                channel.createUpdater().setAutoArchiveDuration(AutoArchiveDuration.ONE_HOUR).update().exceptionally(new ExceptionHandler<>());
            }
            return;
        }

        if (thread.getUser() != messageAuthor.getId()) {
            event.getMessage().delete().exceptionally(new ExceptionHandler<>());
            return;
        }
        event.getMessage().reply("<@%s>, please follow the instructions of the bot to be able to write messages in this thread!".formatted(messageAuthor.getId())).thenAccept(message -> {
            Main.EXECUTOR.schedule(() -> {
                event.getMessage().delete().exceptionally(new ExceptionHandler<>());
            }, 3, TimeUnit.SECONDS);
            Main.EXECUTOR.schedule(() -> {
                message.delete().exceptionally(new ExceptionHandler<>());
            }, 10, TimeUnit.SECONDS);
        });

    }

    private static void onThreadCreated(ServerThreadChannel thread, User user) {
        thread.sendMessage(
                new EmbedBuilder()
                        .setTitle("Support")
                        .setDescription("""
                                **To get support you must first read the [wiki](https://modrepo.de/minecraft/voicechat/wiki) and the [FAQ](https://modrepo.de/minecraft/voicechat/faq).**
                                Please make sure that you have read everything thoroughly and that your problem is certainly not covered there.
                                If this is the case, please generate a support key [here](https://modrepo.de/minecraft/voicechat/wiki/support).
                                After clicking the `Get Support!` button below this message you will be asked to enter the support key.
                                """)
                        .addField("Important", "*By clicking the get support button, you agree that any logs you upload here will be uploaded to [mclo.gs](https://mclo.gs)!*")
                        .addField("Useful Links",
                                String.join(" | ",
                                        "[Mod Description](https://modrinth.com/mod/simple-voice-chat)",
                                        "[FAQ](https://modrepo.de/minecraft/voicechat/faq)",
                                        "[Wiki](https://modrepo.de/minecraft/voicechat/wiki)",
                                        "[Downloads](https://modrepo.de/minecraft/voicechat/downloads)"
                                )
                        )
                        .setColor(Color.BLUE),
                ActionRow.of(
                        new ButtonBuilder().setCustomId(BUTTON_SUPPORT_KEY).setLabel("Get Support!").setStyle(ButtonStyle.PRIMARY).build(),
                        new ButtonBuilder().setCustomId(BUTTON_ABORT_SUPPORT).setLabel("Nevermind...").setStyle(ButtonStyle.DANGER).build()
                )
        ).exceptionally(new ExceptionHandler<>());
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
                ).thenAccept(message -> {
                    Main.EXECUTOR.schedule(() -> {
                        message.delete().exceptionally(new ExceptionHandler<>());
                    }, 10, TimeUnit.SECONDS);
                }).exceptionally(new ExceptionHandler<>());
                event.getButtonInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
                return;
            }
            event.getButtonInteraction().getMessage().createUpdater().removeAllComponents().applyChanges().thenAccept(message -> {
                event.getButtonInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
            }).exceptionally(new ExceptionHandler<>());

            thread.sendMessage(
                    new EmbedBuilder()
                            .setTitle("Success")
                            .setDescription("""
                                    Staff will now be notified.
                                     
                                    Please note that timezones exist and people might not be available instantly.
                                    """)
                            .setColor(Color.GREEN),
                    ActionRow.of(SupportThreadUtils.closeThreadButton())
            ).exceptionally(new ExceptionHandler<>());
            thread.createUpdater().setAutoArchiveDuration(AutoArchiveDuration.ONE_DAY).update().exceptionally(new ExceptionHandler<>());
            SupportThreadUtils.notifyStaff(thread, t);
        }).exceptionally(new ExceptionHandler<>());
    }

    public static void onModalSubmit(ModalSubmitEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
            return;
        }

        String id = event.getModalInteraction().getCustomId();
        if (MODAL_SUPPORT_KEY.equals(id)) {
            String value = event.getModalInteraction().getTextInputValueByCustomId(TEXT_FIELD_SUPPORT_KEY).orElse(null);
            onSupportKeyProvided(thread, event.getModalInteraction(), value);
        }
    }

    private static void onSupportKeyProvided(ServerThreadChannel thread, ModalInteraction modalInteraction, String supportKey) {
        long userId = modalInteraction.getUser().getId();
        if (!SupportKey.verifySupportKey(supportKey)) {
            modalInteraction.
                    createImmediateResponder()
                    .setContent("Invalid support key!")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond()
                    .exceptionally(new ExceptionHandler<>());
            return;
        }
        modalInteraction.respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
        thread.sendMessage(new EmbedBuilder().setDescription("The support key of <@%s> is `%s`.".formatted(userId, supportKey)).setColor(Color.GREEN)).exceptionally(new ExceptionHandler<>());

        clearAllComponents(thread).thenAccept(messages -> {
            sendSupportTemplateMessage(thread);
        }).exceptionally(new ExceptionHandler<>());
    }

    private static CompletableFuture<List<Message>> clearAllComponents(ServerThreadChannel thread) {
        List<CompletableFuture<Message>> futures = new ArrayList<>();
        thread.getMessagesAsStream().forEach(message -> {
            if (!message.getType().equals(MessageType.NORMAL)) {
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
        SelectMenuBuilder selectMenuBuilder = new SelectMenuBuilder(ComponentType.SELECT_MENU_STRING, SELECT_MENU_ISSUE)
                .setMinimumValues(1)
                .setMaximumValues(1)
                .setPlaceholder("Select issue");

        for (Issue issue : Issues.ISSUES) {
            selectMenuBuilder.addOption(new SelectMenuOptionBuilder().setLabel(issue.getName()).setValue(issue.getId()).build());
        }

        thread.sendMessage(
                new EmbedBuilder()
                        .setTitle("Select your issue")
                        .setDescription("Please select your issue from the menu below.")
                        .setColor(Color.BLUE),
                ActionRow.of(selectMenuBuilder.build()),
                ActionRow.of(SupportThreadUtils.closeThreadButton())
        ).exceptionally(new ExceptionHandler<>());
    }

    public static void onSelectMenuChoose(SelectMenuChooseEvent event) {
        ServerThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.getInteraction().respondLater().thenAccept(InteractionOriginalResponseUpdater::delete).exceptionally(new ExceptionHandler<>());
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

            ).exceptionally(new ExceptionHandler<>());
            issue.onSelectIssue(thread);
            issue.sendQuestions(thread);
        }).exceptionally(new ExceptionHandler<>());
    }

}
