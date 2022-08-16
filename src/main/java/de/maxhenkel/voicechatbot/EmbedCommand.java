package de.maxhenkel.voicechatbot;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;

import javax.annotation.Nullable;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;

public class EmbedCommand {

    private static final String EMBED_COMMAND = "embed";

    public static void init() {
        CommandRegistry.registerCommand(EMBED_COMMAND, "Sends an embed",
                Collections.singletonList(SlashCommandOption.createStringOption(
                        "embed",
                        "The message",
                        true
                )),
                EmbedCommand::onEmbedCommand,
                PermissionType.ADMINISTRATOR
        );
    }

    private static void onEmbedCommand(SlashCommandCreateEvent event) {
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

        channel.sendMessage(parseEmbedString(value.replace("\\n", "\n")));
        event.getSlashCommandInteraction().createImmediateResponder().setContent("Successfully created embed").setFlags(MessageFlag.EPHEMERAL).respond();
    }

    public static SimpleDateFormat[] DATE_FORMAT = new SimpleDateFormat[]{
            new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss"),
            new SimpleDateFormat("yyyy-MM-dd-HH-mm"),
            new SimpleDateFormat("yyyy-MM-dd-HH"),
            new SimpleDateFormat("yyyy-MM-dd"),
    };

    private static EmbedBuilder parseEmbedString(String embedString) {
        EmbedBuilder builder = new EmbedBuilder();

        StringReader stringReader = new StringReader(embedString);
        Map<String, String> entries = new HashMap<>();
        String firstLine = stringReader.readUntil("<!--");
        for (; ; ) {
            String variable = stringReader.readUntil("-->");
            if (variable == null) {
                break;
            }
            String value = stringReader.readUntil("<!--");
            entries.put(variable, Objects.requireNonNullElse(value, ""));
        }

        if (firstLine != null && !firstLine.isBlank()) {
            builder.setDescription(firstLine);
        }

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (entry.getKey().toLowerCase().contains("title")) {
                builder.setTitle(entry.getValue());
            } else if (entry.getKey().toLowerCase().contains("description")) {
                builder.setDescription(entry.getValue());
            } else if (entry.getKey().toLowerCase().contains("footer")) {
                builder.setFooter(entry.getValue());
            } else if (entry.getKey().toLowerCase().contains("field")) {
                String[] split = entry.getValue().split("\\|", 2);
                if (split.length == 2) {
                    builder.addField(split[0].strip(), split[1].strip());
                }
            } else if (entry.getKey().toLowerCase().contains("timestamp-now")) {
                builder.setTimestampToNow();
            } else if (entry.getKey().toLowerCase().contains("timestamp")) {
                Date date = null;
                for (SimpleDateFormat sdf : DATE_FORMAT) {
                    try {
                        date = sdf.parse(entry.getValue());
                        break;
                    } catch (ParseException e) {
                    }
                }
                if (date != null) {
                    builder.setTimestamp(date.toInstant());
                }
            } else if (entry.getKey().toLowerCase().contains("author")) {
                builder.setAuthor(entry.getValue());
            } else if (entry.getKey().toLowerCase().contains("image")) {
                builder.setImage(entry.getValue());
            } else if (entry.getKey().toLowerCase().contains("color")) {
                builder.setColor(Color.decode(entry.getValue()));
            } else if (entry.getKey().toLowerCase().contains("thumbnail")) {
                builder.setThumbnail(entry.getValue());
            } else if (entry.getKey().toLowerCase().contains("url")) {
                builder.setUrl(entry.getValue());
            }
        }
        return builder;
    }

    private static class StringReader {

        private final String string;
        private int position;

        public StringReader(String string) {
            this.string = string;
            this.position = 0;
        }

        @Nullable
        public String readUntil(String str) {
            if (position >= string.length()) {
                return null;
            }

            int index = string.indexOf(str, position);

            if (index < 0) {
                String ret = string.substring(position).strip();
                position = string.length();
                return ret;
            }

            String ret = string.substring(position, index);

            position = index + str.length();

            return ret.strip();
        }

    }

}
