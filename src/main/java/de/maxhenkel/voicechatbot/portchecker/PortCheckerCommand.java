package de.maxhenkel.voicechatbot.portchecker;

import de.maxhenkel.voicechatbot.CommandRegistry;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.Collections;
import java.util.List;

public class PortCheckerCommand {

    private static final String PORT_CHECKER_COMMAND = "ping";

    public static void init() {
        CommandRegistry.registerCommand(PORT_CHECKER_COMMAND, "Pings a voice chat server",
                Collections.singletonList(SlashCommandOption.createStringOption(
                        "url",
                        "The voice chat server url",
                        true
                )),
                PortCheckerCommand::onCheckPort,
                PermissionType.ADMINISTRATOR
        );
    }

    private static void onCheckPort(SlashCommandCreateEvent event) {
        List<SlashCommandInteractionOption> arguments = event.getSlashCommandInteraction().getArguments();
        if (arguments.size() != 1) {
            return;
        }
        String value = arguments.get(0).getStringValue().orElse(null);
        if (value == null) {
            return;
        }

        TextChannel channel = event.getSlashCommandInteraction().getChannel().orElse(null);
        if (channel == null) {
            return;
        }

        event.getSlashCommandInteraction().createImmediateResponder().respond();
        PortChecker.checkPort(channel, value);
    }

}
