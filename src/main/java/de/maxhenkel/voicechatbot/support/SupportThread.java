package de.maxhenkel.voicechatbot.support;

import de.maxhenkel.voicechatbot.*;
import de.maxhenkel.voicechatbot.db.Thread;
import de.maxhenkel.voicechatbot.support.issues.Issue;
import de.maxhenkel.voicechatbot.support.issues.Issues;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.thread.member.ThreadMemberJoinEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        TextChannel supportChannel = SupportThreadUtils.getChannel(Environment.SUPPORT_CHANNEL_ID);
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
                Collections.singletonList(
                        new OptionData(OptionType.STRING, "issue", "The issue to show", true)
                                .addChoices(Issues.ISSUES.stream().map(issue -> new Command.Choice(issue.getName(), issue.getId())).toList())
                ),
                SupportThread::onIssueCommand);
        CommandRegistry.registerCommand(CLEANUP_COMMAND, "Cleans up old threads", SupportThread::onCleanupCommand, Permission.ADMINISTRATOR);

        supportChannel.getIterableHistory()
                .forEachAsync(message -> {
                    if (message.getAuthor().getIdLong() == supportChannel.getJDA().getSelfUser().getIdLong()) {
                        message.delete().queue();
                    }
                    return true;
                })
                .thenAccept(o -> {
                    supportChannel.sendMessageEmbeds(new EmbedBuilder()
                                    .setTitle("Support")
                                    .setDescription("""
                                            Please press the `Get Support` button to get support.
                                            You will get asked a couple of questions about your issue and then you'll get help by our staff.
                                            """)
                                    .setColor(Color.GREEN)
                                    .build()
                            )
                            .addComponents(ActionRow.of(Button.success(SupportThread.BUTTON_GET_SUPPORT, "Get Support")))
                            .queue();
                })
                .exceptionally(error -> {
                    Main.LOGGER.error("Error iterating messages", error);
                    return null;
                });
    }

    private static void onGetSupportButtonPressed(ButtonInteractionEvent event) {
        TextChannel supportChannel = SupportThreadUtils.getChannel(Environment.SUPPORT_THREAD_CHANNEL_ID);
        if (supportChannel == null) {
            Main.LOGGER.error("Support thread channel not found");
            return;
        }
        User user = event.getInteraction().getUser();
        Thread t = SupportThreadUtils.getThread(user.getIdLong());
        if (t != null) {
            event.getInteraction().reply("Hey %s, you already have a support thread: <#%s>".formatted(user.getAsMention(), t.getThread())).setEphemeral(true).queue();
            return;
        }
        if (ThreadCooldown.isOnCooldown(user.getIdLong())) {
            event.getInteraction().reply("Hey %s, you already recently opened a support thread. Please wait a while to create a new one.".formatted(user.getAsMention())).setEphemeral(true).queue();
            return;
        }
        supportChannel.sendMessage("Support for %s".formatted(user.getAsMention())).queue(message -> {
            message.createThreadChannel("Support thread for %s".formatted(user.getName())).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK).queue(thread -> {
                event.getInteraction().reply("Hey %s, please follow the steps in your support thread: %s".formatted(user.getAsMention(), thread.getAsMention())).setEphemeral(true).queue();
                thread.addThreadMember(user).queue(unused -> {
                    Main.DB.addThread(new Thread(user.getIdLong(), thread.getIdLong()));
                    ThreadCooldown.setCooldown(user.getIdLong());
                    onThreadCreated(thread, user);
                }, new ExceptionHandler());
            }, new ExceptionHandler());
        }, new ExceptionHandler());
    }

    private static void onCleanupCommand(SlashCommandInteractionEvent event) {
        event.reply("Archiving all stale threads...").queue(hook -> {
            AtomicInteger removed = new AtomicInteger();

            Collection<Thread> threads = Main.DB.getThreads();
            if (threads == null) {
                hook.editOriginal("Failed to archive threads.").queue();
                return;
            }

            for (Thread t : threads) {
                ThreadChannel thread = event.getJDA().getThreadChannelById(t.getThread());
                if (thread == null) {
                    Main.DB.removeThread(t.getThread());
                    updateProgress(hook, removed.incrementAndGet());
                    return;
                }
                thread.getHistory().retrievePast(1).queue(messages -> {
                    if (messages.isEmpty() || messages.getFirst().getTimeCreated().toInstant()
                            .isBefore(Instant.now().minus(Environment.SUPPORT_STALE_DAYS, ChronoUnit.DAYS))) {

                        SupportThreadUtils.closeThread(thread, t, event.getJDA().getSelfUser());
                        updateProgress(hook, removed.incrementAndGet());
                    }
                }, new ExceptionHandler());
            }

            Main.EXECUTOR.schedule(() -> {
                hook.editOriginal("Finished archiving %d threads.".formatted(removed.get())).queue();
            }, 5, TimeUnit.SECONDS);
        });
    }

    private static void updateProgress(InteractionHook hook, int count) {
        hook.editOriginal("Archived %d threads...".formatted(count)).queue();
    }

    private static void onIssueCommand(SlashCommandInteractionEvent event) {
        ThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getInteraction();
        if (thread == null) {
            return;
        }
        if (!SupportThreadUtils.isStaff(interaction.getMember())) {
            return;
        }
        List<OptionMapping> options = event.getOptions();
        if (options.size() != 1) {
            return;
        }

        String value = options.getFirst().getAsString();
        Issue issue = Issues.byId(value);
        if (issue == null) {
            return;
        }

        thread.addThreadMember(event.getUser()).queue();
        thread.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Issue type changed")
                        .setDescription("""
                                %s changed the issue type to `%s`.
                                
                                Please provide additional information, so we can help you.
                                """.formatted(event.getUser().getAsMention(), issue.getName()))
                        .setColor(Color.GREEN)
                        .build()
        ).queue();

        MessageEmbed disclaimer = issue.getDisclaimer();
        if (disclaimer != null) {
            thread.sendMessageEmbeds(disclaimer).queue();
        }

        issue.sendQuestions(thread);

        event.reply(issue.getName())
                .setEphemeral(true)
                .queue();
    }

    private static void onUnlockCommand(SlashCommandInteractionEvent event) {
        ThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getInteraction();
        if (thread == null) {
            return;
        }
        if (!SupportThreadUtils.isStaff(interaction.getMember())) {
            return;
        }

        Main.DB.unlockThread(thread.getIdLong());
        SupportThreadUtils.notifyStaff(thread).thenAccept(unused -> {
            Thread t = Main.DB.getThread(thread.getIdLong());
            if (t != null) {
                SupportThreadUtils.updateStaffNotification(t, "%s unlocked the thread".formatted(event.getInteraction().getUser().getAsMention()));
            }
        }).exceptionally(throwable -> {
            Main.LOGGER.error("Failed to notify staff", throwable);
            return null;
        });
        interaction.reply("Thread unlocked")
                .setEphemeral(true)
                .setContent("Thread unlocked")
                .queue();
    }

    private static void onCloseCommand(SlashCommandInteractionEvent event) {
        ThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        SlashCommandInteraction interaction = event.getInteraction();
        if (thread == null) {
            event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
            return;
        }
        if (!SupportThreadUtils.isStaff(interaction.getMember())) {
            return;
        }
        Thread t = Main.DB.getThread(thread.getIdLong());
        if (t != null) {
            SupportThreadUtils.closeThread(thread, t, interaction.getUser());
            interaction.reply("Thread closed")
                    .setEphemeral(true)
                    .queue();
        } else {
            interaction.reply("Can't find thread")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private static void onConfirmAnswersButtonPressed(ButtonInteractionEvent event) {
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        ThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (t == null || thread == null) {
            event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
            return;
        }
        onConfirmAnswers(thread, t, event);
    }

    private static void onAbortSupportButtonPressed(ButtonInteractionEvent event) {
        SupportThreadUtils.closeThread(SupportThreadUtils.getThread(event.getInteraction()), SupportThreadUtils.getThreadIfOwner(event.getInteraction()), event.getInteraction().getUser());
        event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
    }

    private static void onSupportKeyButtonPressed(ButtonInteractionEvent event) {
        Thread thread = SupportThreadUtils.getThreadIfOwner(event);
        if (thread == null) {
            event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
            return;
        }

        Modal modal = Modal.create(MODAL_SUPPORT_KEY, "Support Key")
                .addComponents(
                        ActionRow.of(
                                TextInput.create(TEXT_FIELD_SUPPORT_KEY, "Your Support Key", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .build()
                        )
                ).build();

        event.replyModal(modal).queue();
    }

    public static void onMessage(MessageReceivedEvent event) {
        ThreadChannel channel = SupportThreadUtils.getThread(event.getChannel());
        if (channel == null) {
            return;
        }

        User messageAuthor = event.getAuthor();
        if (messageAuthor.isBot() || messageAuthor.isSystem()) {
            return;
        }
        Thread thread = Main.DB.getThread(channel.getIdLong());

        if (thread == null) {
            event.getMessage().delete().queue(unused -> {
                channel.getManager().setArchived(true).setLocked(true).queue();
            }, new ExceptionHandler());
            return;
        }

        if (thread.isUnlocked() && event.getMember() != null) {
            return;
        }

        if (thread.getUser() != messageAuthor.getIdLong()) {
            event.getMessage().delete().queue();
            return;
        }
        event.getMessage().reply("%s, please follow the instructions of the bot to be able to write messages in this thread!".formatted(messageAuthor.getAsMention())).queue(message -> {
            Main.EXECUTOR.schedule(() -> {
                event.getMessage().delete().queue();
            }, 3, TimeUnit.SECONDS);
            Main.EXECUTOR.schedule(() -> {
                message.delete().queue();
            }, 10, TimeUnit.SECONDS);
        }, new ExceptionHandler());

    }

    private static void onThreadCreated(ThreadChannel thread, User user) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Support")
                .setDescription("""
                        **To get support you must first read the [wiki](https://modrepo.de/minecraft/voicechat/wiki) and the [FAQ](https://modrepo.de/minecraft/voicechat/faq).**
                        Please make sure that you have read everything thoroughly and that your problem is certainly not covered there.
                        If this is the case, please generate a support key [here](https://modrepo.de/minecraft/voicechat/wiki/support).
                        After clicking the `Get Support!` button below this message you will be asked to enter the support key.
                        """)
                .addField("Important", "*By clicking the get support button, you agree that any logs you upload here will be uploaded to [mclo.gs](https://mclo.gs)!*", false)
                .addField("Useful Links",
                        String.join(" | ",
                                "[Mod Description](https://modrinth.com/mod/simple-voice-chat)",
                                "[FAQ](https://modrepo.de/minecraft/voicechat/faq)",
                                "[Wiki](https://modrepo.de/minecraft/voicechat/wiki)",
                                "[Downloads](https://modrepo.de/minecraft/voicechat/downloads)"
                        ), false)
                .setColor(Color.BLUE);

        Button supportButton = Button.of(ButtonStyle.PRIMARY, BUTTON_SUPPORT_KEY, "Get Support!");
        Button abortButton = Button.of(ButtonStyle.DANGER, BUTTON_ABORT_SUPPORT, "Nevermind...");

        thread.sendMessageEmbeds(embedBuilder.build())
                .addComponents(ActionRow.of(supportButton, abortButton))
                .queue();
    }

    private static void onConfirmAnswers(ThreadChannel thread, Thread t, ButtonInteractionEvent event) {
        thread.getHistory().retrievePast(10).queue(messages -> {
            if (messages.stream()
                    .noneMatch(msg -> msg.getAuthor().getIdLong() == t.getUser())) {
                thread.sendMessageEmbeds(
                        new EmbedBuilder()
                                .setTitle("No messages")
                                .setDescription("No messages have been detected.")
                                .setColor(Color.RED)
                                .build()
                ).queue(warningMsg -> {
                    Main.EXECUTOR.schedule(() -> {
                        ThreadChannel refreshedThread = Main.API.getThreadChannelById(thread.getIdLong());
                        if (refreshedThread != null) {
                            if (!refreshedThread.isLocked() && !refreshedThread.isArchived()) {
                                warningMsg.delete().queue();
                            }
                        }
                    }, 10, TimeUnit.SECONDS);
                });

                event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
                return;
            }

            event.getInteraction().editComponents(Collections.emptyList()).queue();

            thread.sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setTitle("Success")
                                    .setDescription("""
                                            Staff will now be notified.
                                            
                                            Please note that timezones exist and people might not be available instantly.
                                            """)
                                    .setColor(Color.GREEN)
                                    .build()
                    ).addComponents(ActionRow.of(SupportThreadUtils.closeThreadButton()))
                    .queue();

            SupportThreadUtils.notifyStaff(thread, t);
        }, new ExceptionHandler());
    }

    public static void onModalSubmit(ModalInteractionEvent event) {
        ThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
            return;
        }

        String id = event.getInteraction().getModalId();
        if (MODAL_SUPPORT_KEY.equals(id)) {
            ModalMapping mapping = event.getValue(TEXT_FIELD_SUPPORT_KEY);
            if (mapping == null) {
                return;
            }
            onSupportKeyProvided(thread, event.getInteraction(), mapping.getAsString());
        }
    }

    private static void onSupportKeyProvided(ThreadChannel thread, ModalInteraction modalInteraction, String supportKey) {
        if (!SupportKey.verifySupportKey(supportKey)) {
            modalInteraction.reply("Invalid support key!")
                    .setEphemeral(true).queue();
            return;
        }
        modalInteraction.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
        thread.sendMessageEmbeds(new EmbedBuilder().setDescription("The support key of %s is `%s`.".formatted(modalInteraction.getUser().getAsMention(), supportKey)).setColor(Color.GREEN).build()).queue();

        clearAllComponents(thread).thenAccept(messages -> {
            sendSupportTemplateMessage(thread);
        }).exceptionally(throwable -> {
            Main.LOGGER.error("Failed to clear all components", throwable);
            return null;
        });
    }

    private static CompletableFuture<List<Message>> clearAllComponents(ThreadChannel thread) {
        List<CompletableFuture<Message>> futures = new ArrayList<>();
        thread.getIterableHistory().forEach(message -> {
            if (message.getType() != MessageType.DEFAULT) {
                return;
            }
            if (message.getAuthor().getIdLong() == thread.getJDA().getSelfUser().getIdLong()) {
                futures.add(message.editMessageComponents(Collections.emptyList()).submit());
            }
        });

        return all(futures);
    }

    private static <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(ignored -> Collections.emptyList());
    }

    private static void sendSupportTemplateMessage(ThreadChannel thread) {
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create(SELECT_MENU_ISSUE)
                .setMinValues(1)
                .setMaxValues(1)
                .setPlaceholder("Select issue");

        for (Issue issue : Issues.ISSUES) {
            selectMenuBuilder.addOptions(SelectOption.of(issue.getName(), issue.getId()));
        }

        thread.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Select your issue")
                        .setDescription("Please select your issue from the menu below.")
                        .setColor(Color.BLUE)
                        .build()
        ).addComponents(
                ActionRow.of(selectMenuBuilder.build()),
                ActionRow.of(SupportThreadUtils.closeThreadButton())
        ).queue();
    }

    public static void onSelectMenuChoose(StringSelectInteractionEvent event) {
        ThreadChannel thread = SupportThreadUtils.getThread(event.getInteraction());
        if (thread == null) {
            return;
        }
        Thread t = SupportThreadUtils.getThreadIfOwner(event.getInteraction());
        if (t == null) {
            event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
            return;
        }

        if (!SELECT_MENU_ISSUE.equals(event.getInteraction().getComponentId())) {
            return;
        }
        List<String> chosenOptions = event.getValues();

        if (chosenOptions.isEmpty()) {
            return;
        }

        String selection = chosenOptions.getFirst();

        clearAllComponents(thread).thenAccept(messages -> {
            Issue issue = Issues.byId(selection);
            if (issue == null) {
                return;
            }

            thread.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("`%s` selected.".formatted(issue.getName()))
                    .setDescription("""
                            Please provide additional information, so we can help you.
                            Once you did, you will get help by our team.
                            """)
                    .setColor(Color.GREEN)
                    .build()

            ).queue();

            MessageEmbed disclaimer = issue.getDisclaimer();
            if (disclaimer != null) {
                thread.sendMessageEmbeds(disclaimer).queue();
            }

            issue.sendQuestions(thread);
        }).exceptionally(throwable -> {
            Main.LOGGER.error("Failed to clear all components", throwable);
            return null;
        });
    }

    public static void onThreadMemberJoin(ThreadMemberJoinEvent event) {
        ThreadChannel threadChannel = event.getThread();
        Thread thread = SupportThreadUtils.getThreadFromThreadId(threadChannel.getIdLong());
        if (thread == null) {
            return;
        }
        Member member = event.getMember();
        if (!SupportThreadUtils.isStaff(member)) {
            return;
        }
        SupportThreadUtils.updateStaffNotification(thread, "%S joined ✅".formatted(member.getAsMention()));
    }

    public static void onThreadMemberLeaveServer(GuildMemberRemoveEvent event) {
        Thread t = SupportThreadUtils.getThread(event.getUser().getIdLong());
        if (t == null) {
            return;
        }
        ThreadChannel thread = Main.API.getThreadChannelById(t.getThread());
        if (thread == null) {
            Main.DB.removeThread(t.getThread());
            return;
        }
        thread.sendMessageEmbeds(
                new EmbedBuilder()
                        .setDescription("The owner of this thread left the server.")
                        .setColor(Color.RED)
                        .build()
        ).queue();
        SupportThreadUtils.closeThread(thread, t, Main.API.getSelfUser());
    }

    public static void cleanupUninitializedThreads() {
        Collection<Thread> threads = Main.DB.getThreads();
        if (threads == null) {
            Main.LOGGER.error("Failed to clean up uninitialized threads");
            return;
        }

        for (Thread t : threads) {
            ThreadChannel thread = Main.API.getThreadChannelById(t.getThread());
            if (thread == null) {
                Main.DB.removeThread(t.getThread());
                return;
            }
            if (t.getNotifyMessage() > 0L) {
                continue;
            }
            thread.getHistory().retrievePast(1).queue(messages -> {
                if (messages.isEmpty() || messages.getFirst().getTimeCreated().toInstant()
                        .isBefore(Instant.now().minus(Environment.SUPPORT_UNINITIALIZED_HOURS, ChronoUnit.HOURS))) {

                    thread.sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setDescription("""
                                            This ticket was not submitted and did not have any activity in the last %d hours.
                                            """.formatted(Environment.SUPPORT_UNINITIALIZED_HOURS))
                                    .setColor(Color.RED)
                                    .build()
                    ).queue();
                    SupportThreadUtils.closeThread(thread, t, Main.API.getSelfUser());
                }
            }, new ExceptionHandler());
        }
    }
}
