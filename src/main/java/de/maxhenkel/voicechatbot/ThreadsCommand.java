package de.maxhenkel.voicechatbot;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ThreadsCommand {

    private static final String THREADS_COMMAND = "threads";

    public static void init() {
        CommandRegistry.registerCommand(THREADS_COMMAND, "Get all open threads",
                ThreadsCommand::onThreadsCommand
        );
    }

    private static void onThreadsCommand(SlashCommandInteractionEvent event) {
        StringBuilder sb = new StringBuilder();
        Main.DB.getThreads(t -> {
            if (t.getNotifyMessage() <= 0L) {
                return;
            }
            ThreadChannel thread = Main.API.getThreadChannelById(t.getThread());
            if (thread == null) {
                return;
            }
            sb.append("<#%s>".formatted(thread.getId()));
            if (thread.getMembers().stream().anyMatch(threadMember -> threadMember.getIdLong() == event.getInteraction().getUser().getIdLong())) {
                sb.append(" ✅");
            }
            sb.append("\n");
        });

        if (sb.isEmpty()) {
            event.getInteraction().reply("No open threads").setEphemeral(true).queue();
            return;
        }

        event.getInteraction().reply(sb.toString()).setEphemeral(true).queue();
    }

}
