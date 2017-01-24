package com.zulip.android.filters;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.stmt.Where;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;
import com.zulip.android.util.Constants;

import org.json.JSONException;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NarrowFilterByDate implements NarrowFilter {

    public static final Parcelable.Creator<NarrowFilterByDate> CREATOR = new Parcelable.Creator<NarrowFilterByDate>() {
        public NarrowFilterByDate createFromParcel(Parcel in) {
            return new NarrowFilterByDate(new Date(in.readLong()));
        }

        public NarrowFilterByDate[] newArray(int size) {
            return new NarrowFilterByDate[size];
        }
    };
    private Date date = new Date();
    private static Calendar calendar = Calendar.getInstance();
    private static Calendar calendar2 = Calendar.getInstance();

    public NarrowFilterByDate() {
    }

    public NarrowFilterByDate(Date date) {
        this.date = date;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date.getTime());
    }

    public Date getDate() {
        return this.date;
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date fromDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date toDate = calendar.getTime();

        where.between(Message.TIMESTAMP_FIELD, fromDate, toDate);
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        return isSameDay(date, new Date(msg.getTimestamp().getTime()));
    }

    @Override
    public String getTitle() {
        return isSameDay(date, new Date()) ? ZulipApp.get().getString(R.string.today_messages) : ZulipApp.get().getString(R.string.messages);
    }

    @Override
    public String getSubtitle() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        return dateFormat.format(date);
    }

    @Override
    public Stream getComposeStream() {
        return null;
    }

    @Override
    public String getComposePMRecipient() {
        return null;
    }

    @Override
    public String getJsonFilter() throws JSONException {
        return "{}";
    }

    @Override
    public boolean equals(NarrowFilter filter) {
        if (filter instanceof NarrowFilterByDate) {
            NarrowFilterByDate filterByDate = (NarrowFilterByDate) filter;
            return NarrowFilterByDate.isSameDay(this.getDate(),
                    filterByDate.getDate());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{}";
    }

    /**
     * Checks two dates are of same day or not
     *
     * @param date1 Date date1 to be compared with date2
     * @param date2 Date date2 to be compared with date1
     * @return boolean
     */
    private static boolean isSameDay(Date date1, Date date2) {
        calendar.setTime(date1);
        calendar2.setTime(date2);
        return calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);
    }
}
