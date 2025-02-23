package de.maxhenkel.voicechatbot.portchecker;

import de.maxhenkel.voicechatbot.Environment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortChecker {

    private static final byte MAGIC_BYTE = (byte) 0b11111111;
    private static final UUID CHECK_V1 = UUID.fromString("58bc9ae9-c7a8-45e4-a11c-efbb67199425");

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void checkPort(TextChannel channel, String url) {
        MessageAppender appender = new MessageAppender(channel, url);
        appender.addLog("Pinging voice chat server '%s'".formatted(url)).startEmbed();
        checkPortAsync(url, appender);
    }

    public static void checkPortAsync(String serverUrl, MessageAppender appender) {
        URI uri;
        try {
            uri = new URI("voicechat://" + serverUrl);
        } catch (URISyntaxException e) {
            appender.addLog("Invalid URL").finishWithError("Invalid URL.").updateMessage();
            return;
        }
        executor.execute(() -> {
            try {
                InetAddress inetAddress = InetAddress.getByName(uri.getHost());
                if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
                    appender.addLog("Local addresses are not allowed").finishWithError("Local addresses are not allowed.").updateMessage();
                    return;
                }
                int port = uri.getPort();
                if (port < 0) {
                    port = Environment.DEFAULT_VOICE_CHAT_PORT;
                } else if (port > 65535) {
                    port = Environment.DEFAULT_VOICE_CHAT_PORT;
                }
                SocketAddress address = new InetSocketAddress(inetAddress, port);
                appender.addLog("Resolved address to '%s' and port to '%s'".formatted(inetAddress.getHostAddress(), port)).updateMessage();
                checkPortSync(address, appender);
            } catch (SocketException e) {
                appender.addLog("Failed to open UDP socket").finishWithError("Failed to open UDP socket.").updateMessage();
            } catch (UnknownHostException e) {
                appender.addLog("Invalid or unknown host").finishWithError("Invalid or unknown host.").updateMessage();
            }
        });
    }

    private static void checkPortSync(SocketAddress address, MessageAppender appender) throws SocketException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(Environment.PORT_CHECKER_TIMEOUT);
            for (int i = 0; i < Environment.PORT_CHECKER_ATTEMPTS; i++) {
                try {
                    sendPing(socket, address);
                } catch (Exception e) {
                    appender.addLog("Failed to send ping").finishWithError("Failed to send ping.").updateMessage();
                    return;
                }
                try {
                    Pong pong = receivePong(socket);
                    int ping = (int) (System.currentTimeMillis() - pong.getTimestamp());
                    appender
                            .addLog("Got a response in %s ms".formatted(ping))
                            .finishSuccess("Voice chat port is open.\nPing `%s ms`.".formatted(ping))
                            .updateMessage();
                    return;
                } catch (SocketTimeoutException e) {
                    appender.addLog("Attempting to ping (%s/%s)".formatted(i + 1, Environment.PORT_CHECKER_ATTEMPTS)).updateMessage();
                } catch (Exception e) {
                    appender.addLog("Failed to read ping response").finishWithError("Failed to read ping response.").updateMessage();
                    return;
                }
            }
            appender
                    .addLog("Timed out after %s attempts".formatted(Environment.PORT_CHECKER_ATTEMPTS))
                    .finishWithUnsuccessful("Timed out after %s attempts.".formatted(Environment.PORT_CHECKER_ATTEMPTS))
                    .updateMessage();
        }
    }

    private static void sendPing(DatagramSocket socket, SocketAddress address) throws IOException {
        Ping ping = new Ping(UUID.randomUUID(), System.currentTimeMillis());
        ByteBuf byteBuf = Unpooled.buffer(4096);
        ping.write(byteBuf);
        byte[] byteArray = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(byteArray);
        send(socket, address, byteArray);
    }

    private static void send(DatagramSocket socket, SocketAddress address, byte[] payload) throws IOException {
        ByteBuf byteBuf = Unpooled.buffer(4096);

        byteBuf.writeByte(MAGIC_BYTE);
        ByteBufUtils.writeUuid(byteBuf, CHECK_V1);
        ByteBufUtils.writeByteArray(byteBuf, payload);

        byte[] byteArray = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(byteArray);

        DatagramPacket sendPacket = new DatagramPacket(byteArray, byteArray.length, address);

        socket.send(sendPacket);
    }

    private static Pong receivePong(DatagramSocket socket) throws IOException {
        ByteBuf received = receive(socket);
        return Pong.read(received);
    }

    private static ByteBuf receive(DatagramSocket socket) throws IOException {
        byte[] receiveData = new byte[4096];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        return Unpooled.wrappedBuffer(receiveData, 0, receivePacket.getLength());
    }

}
