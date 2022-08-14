package de.maxhenkel.voicechatbot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Date {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String currentDate() {
        return DATE_FORMAT.format(Calendar.getInstance().getTime());
    }

}
