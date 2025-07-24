package de.maxhenkel.voicechatbot.portchecker.v1;

import de.maxhenkel.voicechatbot.Environment;
import de.maxhenkel.voicechatbot.portchecker.PortCheckerBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.io.IOException;
import java.net.*;
import java.util.UUID;

public class PortChecker extends PortCheckerBase {

    private static final UUID CHECK_V1 = UUID.fromString("58bc9ae9-c7a8-45e4-a11c-efbb67199425");

    public PortChecker(MessageChannelUnion channel, String url) {
        super(channel, url);
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected void checkPortSync(SocketAddress address) throws SocketException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(Environment.PORT_CHECKER_TIMEOUT);
            boolean success = false;
            int lowestPing = -1;
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
                    appender.addLog("Got a response in %s ms".formatted(ping)).updateMessage();
                    success = true;
                    if (lowestPing < 0 || ping < lowestPing) {
                        lowestPing = ping;
                    }
                    Thread.sleep(Environment.PORT_CHECKER_TIMEOUT);
                } catch (SocketTimeoutException e) {
                    appender.addLog("Attempting to ping (%s/%s)".formatted(i + 1, Environment.PORT_CHECKER_ATTEMPTS)).updateMessage();
                } catch (Exception e) {
                    appender.addLog("Failed to read ping response").finishWithError("Failed to read ping response.").updateMessage();
                    return;
                }
            }
            if (success) {
                appender.finishSuccess("Voice chat port is open.\nPing `%s ms`.".formatted(lowestPing)).updateMessage();
            } else {
                appender
                        .addLog("Timed out after %s attempts".formatted(Environment.PORT_CHECKER_ATTEMPTS))
                        .finishWithUnsuccessful("Timed out after %s attempts.".formatted(Environment.PORT_CHECKER_ATTEMPTS))
                        .updateMessage();
            }
        }
    }

    private static void sendPing(DatagramSocket socket, SocketAddress address) throws IOException {
        Ping ping = new Ping(UUID.randomUUID(), System.currentTimeMillis());
        ByteBuf byteBuf = Unpooled.buffer(4096);
        ping.write(byteBuf);
        byte[] byteArray = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(byteArray);
        send(socket, address, CHECK_V1, byteArray);
    }

    private static Pong receivePong(DatagramSocket socket) throws IOException {
        ByteBuf received = receive(socket);
        return Pong.read(received);
    }

}
