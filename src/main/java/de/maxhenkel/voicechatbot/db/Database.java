package de.maxhenkel.voicechatbot.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import de.maxhenkel.voicechatbot.Environment;
import de.maxhenkel.voicechatbot.Main;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class Database {

    private final ConnectionSource connectionSource;
    private final Dao<Thread, Long> threads;

    public Database() throws SQLException {
        connectionSource = new JdbcConnectionSource("jdbc:sqlite:%s".formatted(Environment.DATABASE_PATH));

        TableUtils.createTableIfNotExists(connectionSource, Thread.class);
        threads = DaoManager.createDao(connectionSource, Thread.class);
    }

    public boolean addThread(Thread thread) {
        if (getThreadByUser(thread.getUser()) != null) {
            return false;
        }
        if (getThread(thread.getThread()) != null) {
            return false;
        }
        try {
            threads.create(thread);
            return true;
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to add thread", e);
            return false;
        }
    }

    public boolean removeThread(long threadId) {
        try {
            threads.deleteById(threadId);
            return true;
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to remove thread", e);
            return false;
        }
    }

    @Nullable
    public Thread getThreadByUser(long userId) {
        try {
            List<Thread> t = threads.queryForEq("user", userId);
            return t.isEmpty() ? null : t.getFirst();
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to get thread by user", e);
            return null;
        }
    }

    @Nullable
    public Thread getThread(long threadId) {
        try {
            return threads.queryForId(threadId);
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to get thread", e);
            return null;
        }
    }

    public boolean unlockThread(long threadId) {
        Thread t = getThread(threadId);
        if (t == null) {
            return false;
        }
        t.setUnlocked(true);
        try {
            threads.update(t);
            return true;
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to unlock thread", e);
            return false;
        }
    }

    public boolean setNotifyMessage(long threadId, long notifyMessage) {
        Thread t = getThread(threadId);
        if (t == null) {
            return false;
        }
        t.setNotifyMessage(notifyMessage);
        try {
            threads.update(t);
            return true;
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to set notify message", e);
            return false;
        }
    }

    @Nullable
    public Collection<Thread> getThreads() {
        try {
            return threads.queryForAll();
        } catch (SQLException e) {
            Main.LOGGER.error("Failed to get threads", e);
            return null;
        }
    }

}
