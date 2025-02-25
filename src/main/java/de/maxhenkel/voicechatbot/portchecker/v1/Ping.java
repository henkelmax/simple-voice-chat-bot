package de.maxhenkel.voicechatbot.portchecker.v1;

import de.maxhenkel.voicechatbot.portchecker.ByteBufUtils;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class Ping {

    protected UUID id;
    protected long timestamp;

    public Ping(UUID id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void write(ByteBuf buf) {
        ByteBufUtils.writeUuid(buf, id);
        buf.writeLong(timestamp);
    }

}
