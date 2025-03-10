package de.maxhenkel.voicechatbot;

import de.maxhenkel.voicechatbot.db.Thread;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Collection;
import java.util.List;

public class ThreadsCommand {

    private static final String THREADS_COMMAND = "threads";

    public static void init() {
        CommandRegistry.registerCommand(THREADS_COMMAND, "Get all open threads",
                ThreadsCommand::onThreadsCommand
        );
    }

    private static void onThreadsCommand(SlashCommandInteractionEvent event) {
        event.getInteraction().deferReply(true).queue();
        StringBuilder sb = new StringBuilder();
        Collection<Thread> threads = Main.DB.getThreads();
        if (threads == null) {
            event.getInteraction().reply("Failed to get threads").setEphemeral(true).queue();
            return;
        }
        for (Thread t : threads) {
            if (t.getNotifyMessage() <= 0L) {
                continue;
            }
            ThreadChannel thread = Main.API.getThreadChannelById(t.getThread());
            if (thread == null) {
                continue;
            }
            sb.append("<#%s>".formatted(thread.getId()));
            List<ThreadMember> members = thread.retrieveThreadMembers().stream().toList();
            if (members.stream().anyMatch(threadMember -> threadMember.getIdLong() == event.getInteraction().getUser().getIdLong())) {
                sb.append(" ✅");
            }
            if (members.stream().noneMatch(threadMember -> threadMember.getIdLong() == t.getUser())) {
                sb.append(" \uD83C\uDFC3");
            }
            sb.append("\n");
        }

        if (sb.isEmpty()) {
            event.getHook().editOriginal("No open threads").queue();
            return;
        }

        event.getHook().editOriginal(sb.toString()).queue();
    }

}
