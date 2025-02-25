package de.maxhenkel.voicechatbot.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "threads")
public class Thread {

    @DatabaseField
    private long user;
    @DatabaseField(id = true)
    private long thread;
    /**
     * If the user is allowed to send messages in the thread
     */
    @DatabaseField
    private boolean unlocked;
    @DatabaseField
    private long notifyMessage;

    public Thread(long user, long thread) {
        this.user = user;
        this.thread = thread;
    }

    public Thread() {

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

    public long getNotifyMessage() {
        return notifyMessage;
    }

    public void setNotifyMessage(long notifyMessage) {
        this.notifyMessage = notifyMessage;
    }

}
