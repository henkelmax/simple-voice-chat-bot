package de.maxhenkel.voicechatbot;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupportKey {

    private static final Pattern SUPPORT_KEY_PATTERN = Pattern.compile("^S-(?<numbers>[1-9]+)$");

    public static boolean verifySupportKey(String supportKey) {
        if (supportKey == null) {
            return false;
        }

        Matcher matcher = SUPPORT_KEY_PATTERN.matcher(supportKey);
        if (!matcher.matches()) {
            return false;
        }

        String numbers = matcher.group("numbers");

        return Arrays.stream(numbers.split("")).map(Integer::parseInt).reduce(0, Integer::sum) == 69; // Nice!
    }

}
