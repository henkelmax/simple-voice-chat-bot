package de.maxhenkel.voicechatbot;

import com.google.gson.Gson;
import de.maxhenkel.voicechatbot.db.Thread;
import de.maxhenkel.voicechatbot.support.SupportThreadUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

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
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class LogUploader {

    private static final String[] LOG_FILE_FORMATS = new String[]{"log", "txt", "gz"};

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
            return;
        }

        if (!thread.isUnlocked()) {
            return;
        }

        for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            String fileName = attachment.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            if (Arrays.stream(LOG_FILE_FORMATS).anyMatch(s -> s.equalsIgnoreCase(extension))) {
                attachment.getProxy().download().thenAccept(inputStream -> {
                    try {
                        onLog(channel, event.getMessage(), attachment, inputStream.readAllBytes(), fileName, extension);
                    } catch (IOException e) {
                        Main.LOGGER.error("Failed to read log file", e);
                    }
                });
            }
        }
    }

    private static void onLog(ThreadChannel channel, Message message, Message.Attachment attachment, byte[] data, String fileName, String extension) {
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
            channel.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("Log uploaded to mclo.gs")
                    .setFooter("Uploaded by %s".formatted(message.getAuthor().getName()))
                    .setAuthor(message.getAuthor().getName())
                    .setTimestamp(Instant.now())
                    .setColor(Color.GREEN).build()
            ).addComponents(ActionRow.of(
                    Button.link(url, "View logs")
            )).queue();
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
            if (!builder.isEmpty()) {
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
