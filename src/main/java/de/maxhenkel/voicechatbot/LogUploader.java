package de.maxhenkel.voicechatbot;

import com.google.gson.Gson;
import de.maxhenkel.voicechatbot.db.Thread;
import de.maxhenkel.voicechatbot.support.SupportThreadUtils;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.ButtonBuilder;
import org.javacord.api.entity.message.component.ButtonStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class LogUploader {

    private static final String[] LOG_FILE_FORMATS = new String[]{"log", "txt", "gz"};

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
            return;
        }

        if (!thread.isUnlocked()) {
            return;
        }

        for (MessageAttachment attachment : event.getMessage().getAttachments()) {
            String fileName = attachment.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            if (Arrays.stream(LOG_FILE_FORMATS).anyMatch(s -> s.equalsIgnoreCase(extension))) {
                attachment.asByteArray().thenAccept(bytes -> {
                    onLog(attachment, bytes, fileName, extension);
                });

            }
        }
    }

    private static void onLog(MessageAttachment attachment, byte[] data, String fileName, String extension) {
        String content;
        if (extension.equalsIgnoreCase("gz")) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data))) {
                content = new String(gzipInputStream.readAllBytes());
            } catch (IOException e) {
                Main.LOGGER.error("Failed to unzip log file", e);
                return;
            }
        } else {
            content = new String(data);
        }

        try {
            String url = upload(content);
            attachment.getMessage().getChannel().sendMessage(new EmbedBuilder()
                            .setTitle("Log uploaded to mclo.gs")
                            .setFooter("Uploaded by %s".formatted(attachment.getMessage().getAuthor().getDisplayName()))
                            .setAuthor(attachment.getMessage().getAuthor())
                            .setTimestampToNow()
                            .setColor(Color.GREEN),
                    ActionRow.of(
                            new ButtonBuilder().setUrl(url).setLabel("View logs").setStyle(ButtonStyle.LINK).build()
                    )
            );
        } catch (Exception e) {
            Main.LOGGER.error("Failed to upload log file", e);
        }
    }

    private static String upload(String content) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(ofFormData(Map.of("content", content)))
                .uri(URI.create("https://api.mclo.gs/1/log"))
                .setHeader("User-Agent", "Simple Voice Chat Bot")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to upload log file. Status code %s".formatted(response.statusCode()));
        }

        Gson gson = new Gson();

        LogResponse logResponse = gson.fromJson(response.body(), LogResponse.class);

        if (logResponse.success) {
            return "https://mclo.gs/%s".formatted(logResponse.id);
        } else {
            throw new IOException("Failed to upload log file: %s".formatted(logResponse.error));
        }
    }

    private static HttpRequest.BodyPublisher ofFormData(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    private static class LogResponse {
        private boolean success;
        private String id;
        private String error;
    }

}
