package com.zenimmersive.android.helper;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeHelper {
    public static String getDateTime() {
        SimpleDateFormat time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        long date = System.currentTimeMillis();

        String timeString1 = time1.format(date);


        return timeString1;
    }

    public static String getUTCDateTime() {
        SimpleDateFormat time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        time1.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeString1 = null;
        try {
            timeString1 = time1.format(new Date());
        } catch (Exception e) {
            return getDateTime();
        }
        return timeString1;
    }

    public static Date getUTCDate(Long currentTimeMillis) {
        // Get the current time in milliseconds since the Unix epoc

        // Create a Calendar instance and set it to the current UTC time
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(currentTimeMillis);

        // Create a Date object from the Calendar instance
        Date utcDate = calendar.getTime();
        return utcDate;
    }

    public static Date getUTCDate() {
        long currentTimeMillis = System.currentTimeMillis();
        return getUTCDate(currentTimeMillis);
    }


    @NotNull
    public static Calendar getUTCCalender() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar;
    }
}
