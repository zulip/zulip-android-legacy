package com.zulip.android.filters;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;

import org.json.JSONException;

import java.sql.SQLException;
import java.util.Date;

public class NarrowFilterToday implements NarrowFilter {

    public static final Parcelable.Creator<NarrowFilterToday> CREATOR = new Parcelable.Creator<NarrowFilterToday>() {
        public NarrowFilterToday createFromParcel(Parcel in) {

            return new NarrowFilterToday();
        }

        public NarrowFilterToday[] newArray(int size) {
            return new NarrowFilterToday[size];
        }
    };

    public NarrowFilterToday() {
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        where.like(Message.TIMESTAMP_FIELD, new SelectArg(new Date()));
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        return (DateUtils.isToday(msg.getTimestamp().getTime()));
    }

    @Override
    public String getTitle() {
        return "Today Messages";
    }

    @Override
    public String getSubtitle() {
        return null;
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
