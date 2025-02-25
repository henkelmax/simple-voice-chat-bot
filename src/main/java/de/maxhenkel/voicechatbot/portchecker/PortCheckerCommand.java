package de.maxhenkel.voicechatbot.portchecker;

import de.maxhenkel.voicechatbot.CommandRegistry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

public class PortCheckerCommand {

    private static final String PING_COMMAND_V1 = "ping";

    public static void init() {
        CommandRegistry.registerCommand(PING_COMMAND_V1, "Pings a voice chat server",
                Collections.singletonList(new OptionData(OptionType.STRING, "url", "The voice chat server url", true)),
                PortCheckerCommand::onCheckPortV1,
                Permission.MODERATE_MEMBERS
        );
    }

    private static void onCheckPortV1(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("url");
        if (option == null) {
            event.reply("Missing required input!").setEphemeral(true).queue();
            return;
        }

        String value = option.getAsString();
        MessageChannelUnion channel = event.getInteraction().getChannel();

        event.deferReply(true).flatMap(InteractionHook::deleteOriginal).queue();
        new de.maxhenkel.voicechatbot.portchecker.v1.PortChecker(channel, value).check();
    }

}
