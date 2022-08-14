package de.maxhenkel.voicechatbot.db;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.maxhenkel.voicechatbot.Environment;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import javax.annotation.Nullable;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class Database {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Thread> threads;

    public Database() {
        CodecRegistry registry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new Thread.ThreadCodec()), MongoClientSettings.getDefaultCodecRegistry());
        MongoClientOptions options = MongoClientOptions.builder().codecRegistry(registry).build();
        mongoClient = new MongoClient(new ServerAddress(Environment.DATABASE_URL), options);
        database = mongoClient.getDatabase(Environment.DATABASE_NAME);
        threads = database.getCollection("threads", Thread.class);
    }

    public boolean addThread(Thread thread) {
        if (getThreadByUser(thread.getUser()) != null) {
            return false;
        }
        if (getThread(thread.getThread()) != null) {
            return false;
        }
        threads.insertOne(thread);
        return true;
    }

    public boolean removeThread(long threadId) {
        return threads.deleteMany(eq("thread", threadId)).getDeletedCount() > 0;
    }

    public boolean removeThreadByUser(long userId) {
        return threads.deleteMany(eq("user", userId)).getDeletedCount() > 0;
    }

    @Nullable
    public Thread getThreadByUser(long userId) {
        return threads.find(eq("user", userId), Thread.class).first();
    }

    @Nullable
    public Thread getThread(long threadId) {
        return threads.find(eq("thread", threadId), Thread.class).first();
    }

    public void unlockUsersThread(long userId) {
        threads.findOneAndUpdate(eq("user", userId), set("unlocked", true));
    }

    public void unlockThread(long threadId) {
        threads.findOneAndUpdate(eq("thread", threadId), set("unlocked", true));
    }

    public void setNotifyMessage(long threadId, long notifyMessage) {
        threads.findOneAndUpdate(eq("thread", threadId), set("notifyMessage", notifyMessage));
    }

}
