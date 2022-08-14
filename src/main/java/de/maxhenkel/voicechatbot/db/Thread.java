package de.maxhenkel.voicechatbot.db;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

public class Thread {

    private long user;
    private long thread;
    private boolean unlocked;

    public Thread(long user, long thread, boolean unlocked) {
        this.user = user;
        this.thread = thread;
        this.unlocked = unlocked;
    }

    public Thread(long user, long thread) {
        this(user, thread, false);
    }

    public long getUser() {
        return user;
    }

    public long getThread() {
        return thread;
    }


    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public void setThread(long thread) {
        this.thread = thread;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public static class ThreadCodec implements Codec<Thread> {

        private final Codec<Document> documentCodec;

        public ThreadCodec() {
            this.documentCodec = new DocumentCodec();
        }

        @Override
        public void encode(BsonWriter writer, Thread value, EncoderContext encoderContext) {
            if (value != null) {
                Document document = new Document();
                document.put("user", value.getUser());
                document.put("thread", value.getThread());
                document.put("unlocked", value.isUnlocked());
                documentCodec.encode(writer, document, encoderContext);
            }
        }

        @Override
        public Thread decode(BsonReader reader, DecoderContext decoderContext) {
            Document document = documentCodec.decode(reader, decoderContext);
            return new Thread(document.getLong("user"), document.getLong("thread"), document.getBoolean("unlocked"));
        }

        @Override
        public Class<Thread> getEncoderClass() {
            return Thread.class;
        }
    }

}
