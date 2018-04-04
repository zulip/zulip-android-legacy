package com.zulip.android.models;

import com.zulip.android.util.DateMethods;

import java.util.Date;
import java.util.Locale;

/**
 * Data Structure for message date separator
 */

public class MessageDateSeparator {

    private Date aboveMessageDate;
    private Date belowMessageDate;

    public MessageDateSeparator(Date aboveMessageDate, Date belowMessageDate) {
        this.aboveMessageDate = aboveMessageDate;
        this.belowMessageDate = belowMessageDate;
    }

    public Date getBelowMessageDate() {
        return belowMessageDate;
    }

    public Date getAboveMessageDate() {
        return aboveMessageDate;
    }

    public String getLeftText() {
        String date = DateMethods.getStringDate(aboveMessageDate);
        return date.isEmpty() ? "" : "\u25B2 " + date.toUpperCase(Locale.US);
    }

    public String getRightText() {
        String date = DateMethods.getStringDate(belowMessageDate);
        return date.isEmpty() ? "" : "\u25BC " + date.toUpperCase(Locale.US);
    }
}
