package de.maxhenkel.voicechatbot;

public class Environment {

    public static final String TOKEN = env("TOKEN", "");
    public static final String DATABASE_URL = env("DB_URL", "localhost:27017");
    public static final String DATABASE_NAME = env("DB_NAME", "discordbot");
    public static final long SERVER_ID = Long.parseLong(env("SERVER_ID", "0"));
    public static final long SUPPORT_CHANNEL_ID = Long.parseLong(env("SUPPORT_CHANNEL_ID", "0"));
    public static final long SUPPORT_THREAD_CHANNEL_ID = Long.parseLong(env("SUPPORT_THREAD_CHANNEL_ID", "0"));
    public static final long SUPPORT_NOTIFICATION_CHANNEL = Long.parseLong(env("SUPPORT_NOTIFICATION_CHANNEL", "0"));
    public static final long SUPPORT_ROLE = Long.parseLong(env("SUPPORT_ROLE", "0"));
    public static final long NO_PING_ROLE = Long.parseLong(env("NO_PING_ROLE", "0"));
    public static final int DEFAULT_VOICE_CHAT_PORT = Integer.parseInt(env("DEFAULT_VOICE_CHAT_PORT", "24454"));
    public static final int PORT_CHECKER_ATTEMPTS = Integer.parseInt(env("PORT_CHECKER_ATTEMPTS", "10"));
    public static final int PORT_CHECKER_TIMEOUT = Integer.parseInt(env("PORT_CHECKER_TIMEOUT", "1000"));
    public static final int SUPPORT_STALE_DAYS = Integer.parseInt(env("SUPPORT_STALE_DAYS", "3"));

    private static String env(String envVar, String def) {
        String var = System.getenv(envVar);
        if (var == null) {
            return def;
        }
        return var;
    }

    public static boolean validate() {
        boolean valid = true;
        if (SERVER_ID <= 0L) {
            Main.LOGGER.error("Invalid SERVER_ID environment variable");
            valid = false;
        }
        if (SUPPORT_CHANNEL_ID <= 0L) {
            Main.LOGGER.error("Invalid SUPPORT_CHANNEL_ID environment variable");
            valid = false;
        }
        if (SUPPORT_THREAD_CHANNEL_ID <= 0L) {
            Main.LOGGER.error("Invalid SUPPORT_THREAD_CHANNEL_ID environment variable");
            valid = false;
        }
        if (SUPPORT_NOTIFICATION_CHANNEL <= 0L) {
            Main.LOGGER.error("Invalid SUPPORT_NOTIFICATION_CHANNEL environment variable");
            valid = false;
        }
        if (SUPPORT_ROLE <= 0L) {
            Main.LOGGER.error("Invalid SUPPORT_ROLE environment variable");
            valid = false;
        }
        return valid;
    }

}
