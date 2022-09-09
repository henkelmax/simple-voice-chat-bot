package de.maxhenkel.voicechatbot.support;

import java.util.HashMap;
import java.util.Map;

public class ThreadCooldown {

    private static final long COOLDOWN = 1000L * 60L * 5L;

    private static final Map<Long, Long> COOLDOWNS = new HashMap<>();

    public static boolean isOnCooldown(long userId) {
        boolean isOnCooldown = COOLDOWNS.containsKey(userId) && COOLDOWNS.get(userId) + COOLDOWN > System.currentTimeMillis();
        if (!isOnCooldown) {
            COOLDOWNS.remove(userId);
        }
        return isOnCooldown;
    }

    public static void setCooldown(long userId) {
        COOLDOWNS.put(userId, System.currentTimeMillis());
    }

    public static void cleanupCooldowns() {
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() + COOLDOWN < System.currentTimeMillis());
    }

}
