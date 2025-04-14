package de.maxhenkel.voicechatbot.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "pings")
public class Ping {

    @DatabaseField(id = true)
    private long user;

    @DatabaseField
    private int pingCount;

    @DatabaseField
    private long lastPing;

    public Ping(long user, int pingCount, long lastPing) {
        this.user = user;
        this.pingCount = pingCount;
        this.lastPing = lastPing;
    }

    public Ping() {

    }

    public long getUser() {
        return user;
    }

    public int getPingCount() {
        return pingCount;
    }

    public void setPingCount(int pingCount) {
        this.pingCount = pingCount;
    }

    public long getLastPing() {
        return lastPing;
    }

    public void setLastPing(long lastPing) {
        this.lastPing = lastPing;
    }
}
