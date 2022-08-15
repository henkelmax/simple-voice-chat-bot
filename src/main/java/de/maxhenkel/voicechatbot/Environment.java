package de.maxhenkel.voicechatbot;

public class Environment {

    public static final String TOKEN = env("TOKEN", "");
    public static final String DATABASE_URL = env("DB_URL", "localhost:27017");
    public static final String DATABASE_NAME = env("DB_NAME", "discordbot");
    public static final long SUPPORT_CHANNEL_ID = Long.parseLong(env("SUPPORT_CHANNEL_ID", "0"));
    public static final long COMMON_ISSUES_CHANNEL_ID = Long.parseLong(env("COMMON_ISSUES_CHANNEL_ID", "0"));
    public static final long SERVER_HOSTING_CHANNEL_ID = Long.parseLong(env("SERVER_HOSTING_CHANNEL_ID", "0"));
    public static final long SUPPORT_NOTIFICATION_CHANNEL = Long.parseLong(env("SUPPORT_NOTIFICATION_CHANNEL", "0"));
    public static final long SUPPORT_LOG_CHANNEL = Long.parseLong(env("SUPPORT_LOG_CHANNEL", "0"));
    public static final long SUPPORT_ROLE = Long.parseLong(env("SUPPORT_ROLE", "0"));

    public static String env(String envVar, String def) {
        String var = System.getenv(envVar);
        if (var == null) {
            return def;
        }
        return var;
    }

}
