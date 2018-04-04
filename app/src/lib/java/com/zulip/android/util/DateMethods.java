package com.zulip.android.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * All methods related to date
 */

public class DateMethods {

    /**
     * Check's whether two date are of same day or not
     *
     * @param date1 compare with date2
     * @param date2 compare with date1
     * @return true if date1 and date2 are of same day
     */
    public static boolean isSameDay(Date date1, Date date2) {
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar.setTime(date1);
        calendar2.setTime(date2);
        return calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Check's whether two date are of same year or not
     *
     * @param date1 compare with date2
     * @param date2 compare with date1
     * @return true if date1 and date2 are of same year
     */
    public static boolean isSameYear(Date date1, Date date2) {
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar.setTime(date1);
        calendar2.setTime(date2);
        return calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
    }

    public static String getStringDate(Date date) {
        if (date == null)
            return "";
        //check for today
        if (isSameDay(date, new Date())) {
            return "Today";
        }
        //check for yesterday
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        if (isSameDay(date, calendar.getTime())) {
            return "Yesterday";
        }
        SimpleDateFormat formatter;
        if (isSameYear(date, new Date())) {
            //return only month and date eg:- MAR 09
            formatter = new SimpleDateFormat("MMM dd", Locale.getDefault());
        } else {
            //return month, date and year eg:- MAR 09, 2017
            formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        }
        return formatter.format(date);
    }
}
