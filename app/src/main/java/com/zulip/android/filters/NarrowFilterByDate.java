package com.zulip.android.filters;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;

import org.json.JSONException;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NarrowFilterByDate implements NarrowFilter {

    public static final Parcelable.Creator<NarrowFilterByDate> CREATOR = new Parcelable.Creator<NarrowFilterByDate>() {
        public NarrowFilterByDate createFromParcel(Parcel in) {

            return new NarrowFilterByDate();
        }

        public NarrowFilterByDate[] newArray(int size) {
            return new NarrowFilterByDate[size];
        }
    };
    private Date date = new Date();

    public NarrowFilterByDate() {
    }

    public NarrowFilterByDate(Date date) {
        this.date = date;
    }

    /**
     * Checks two dates are of same day or not
     *
     * @param date1 long date1 to be compared with date2
     * @param date2 long date2 to be compared with date1
     * @return boolean
     */
    private static boolean isSameDay(long date1, long date2) {
        return date1 / 86400000 == date2 / 86400000;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        where.like(Message.TIMESTAMP_FIELD, new SelectArg(date));
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        return isSameDay(date.getTime(), msg.getTimestamp().getTime());
    }

    @Override
    public String getTitle() {
        return isSameDay(date.getTime(), new Date().getTime()) ? "Today Messages" : "Messages";
    }

    @Override
    public String getSubtitle() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
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
    public String toString() {
        return "{}";
    }
}
