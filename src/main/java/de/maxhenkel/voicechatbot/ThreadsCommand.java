package de.maxhenkel.voicechatbot;

import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;

public class ThreadsCommand {

    private static final String THREADS_COMMAND = "threads";

    public static void init() {
        CommandRegistry.registerCommand(THREADS_COMMAND, "Get all open threads",
                ThreadsCommand::onThreadsCommand
        );
    }

    private static void onThreadsCommand(SlashCommandCreateEvent event) {
        StringBuilder sb = new StringBuilder();
        Main.DB.getThreads(t -> {
            ServerThreadChannel thread = Main.API.getServerThreadChannelById(t.getThread()).orElse(null);
            if (thread == null) {
                Main.DB.removeThread(t.getThread());
                Main.LOGGER.error("Removed thread '{}' from database as it doesn't exist anymore", t.getThread());
                return;
            }
            if (t.getNotifyMessage() <= 0L) {
                return;
            }
            sb.append("<#%s>".formatted(thread.getId()));
            if (thread.getThreadMembers().join().stream().anyMatch(threadMember -> threadMember.getUserId() == event.getInteraction().getUser().getId())) {
                sb.append(" ✅");
            }
            sb.append("\n");
        });

        if (sb.isEmpty()) {
            event.getSlashCommandInteraction().createImmediateResponder().setContent("No open threads").setFlags(MessageFlag.EPHEMERAL).respond();
        }

        event.getSlashCommandInteraction().createImmediateResponder().setContent(sb.toString()).setFlags(MessageFlag.EPHEMERAL).respond();
    }

}
