package de.maxhenkel.voicechatbot.portchecker;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class ByteBufUtils {

    public static void writeVarInt(ByteBuf buf, int varInt) {
        VarIntUtils.write(buf, varInt);
    }

    public static int readVarInt(ByteBuf buf) {
        return VarIntUtils.read(buf);
    }

    public static void writeByteArray(ByteBuf buf, byte[] data) {
        writeVarInt(buf, data.length);
        buf.writeBytes(data);
    }

    public static byte[] readByteArray(ByteBuf buf, int maxSize) {
        int size = readVarInt(buf);
        if (size > maxSize) {
            throw new RuntimeException("Byte array too big (%s>%s)".formatted(size, maxSize));
        }
        byte[] data = new byte[size];
        buf.readBytes(data);
        return data;
    }

    public static void writeUuid(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUuid(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

}
