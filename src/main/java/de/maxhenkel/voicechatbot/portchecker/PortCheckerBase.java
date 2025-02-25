package de.maxhenkel.voicechatbot.portchecker;

import de.maxhenkel.voicechatbot.Environment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.io.IOException;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class PortCheckerBase {

    protected static final byte MAGIC_BYTE = (byte) 0b11111111;

    protected static final ExecutorService executor = Executors.newSingleThreadExecutor();

    protected final MessageChannelUnion channel;
    protected final String url;
    protected final MessageAppender appender;

    public PortCheckerBase(MessageChannelUnion channel, String url) {
        this.channel = channel;
        this.url = url;
        appender = new MessageAppender(channel, url);
    }

    protected abstract int getVersion();

    public void check() {
        appender.addLog("Pinging voice chat server '%s'".formatted(url)).startEmbed();
        appender.addLog("Using ping protocol version %d".formatted(getVersion())).updateMessage();
        checkPortAsync();
    }

    protected void checkPortAsync() {
        URI uri;
        try {
            uri = new URI("voicechat://" + url);
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
                checkPortSync(address);
            } catch (SocketException e) {
                appender.addLog("Failed to open UDP socket").finishWithError("Failed to open UDP socket.").updateMessage();
            } catch (UnknownHostException e) {
                appender.addLog("Invalid or unknown host").finishWithError("Invalid or unknown host.").updateMessage();
            }
        });
    }

    protected abstract void checkPortSync(SocketAddress address) throws SocketException;

    protected static void send(DatagramSocket socket, SocketAddress address, UUID id, byte[] payload) throws IOException {
        ByteBuf byteBuf = Unpooled.buffer(4096);

        byteBuf.writeByte(MAGIC_BYTE);
        ByteBufUtils.writeUuid(byteBuf, id);
        ByteBufUtils.writeByteArray(byteBuf, payload);

        byte[] byteArray = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(byteArray);

        DatagramPacket sendPacket = new DatagramPacket(byteArray, byteArray.length, address);

        socket.send(sendPacket);
    }

    protected static ByteBuf receive(DatagramSocket socket) throws IOException {
        byte[] receiveData = new byte[4096];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        return Unpooled.wrappedBuffer(receiveData, 0, receivePacket.getLength());
    }

}
