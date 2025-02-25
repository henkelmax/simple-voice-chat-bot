package de.maxhenkel.voicechatbot.portchecker.v1;

import de.maxhenkel.voicechatbot.portchecker.ByteBufUtils;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class Pong {

    protected UUID id;
    protected long timestamp;

    public Pong(UUID id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    private Pong() {
    }

    public UUID getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static Pong read(ByteBuf buf) {
        Pong pong = new Pong();
        pong.id = ByteBufUtils.readUuid(buf);
        pong.timestamp = buf.readLong();
        return pong;
    }

}
