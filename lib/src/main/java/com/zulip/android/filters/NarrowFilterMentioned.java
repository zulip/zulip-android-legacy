package com.zulip.android.filters;

import android.os.Parcel;

import com.j256.ormlite.stmt.Where;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;

import org.json.JSONArray;
import org.json.JSONException;

import java.sql.SQLException;
import java.util.Arrays;

public class NarrowFilterMentioned implements NarrowFilter {
    public static final Creator<NarrowFilterMentioned> CREATOR = new Creator<NarrowFilterMentioned>() {
        public NarrowFilterMentioned createFromParcel(Parcel in) {
            return new NarrowFilterMentioned();
        }

        public NarrowFilterMentioned[] newArray(int size) {
            return new NarrowFilterMentioned[size];
        }
    };

    private String name;

    public NarrowFilterMentioned() {
        name = ZulipApp.get().getYou().getName();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {

        where.like(Message.CONTENT_FIELD, name);
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        return msg.getContent().contains(name);
    }

    @Override
    public String getTitle() {
        return ZulipApp.get().getString(R.string.mentions);
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
        return "";
    }

    @Override
    public String getJsonFilter() throws JSONException {
        return (new JSONArray()).put(new JSONArray(Arrays.asList("is", "mentioned"))).toString();
    }

    @Override
    public boolean equals(NarrowFilter filter) {
        if (filter instanceof NarrowFilterMentioned) {
            NarrowFilterMentioned filterPM = (NarrowFilterMentioned) filter;
            return this.getTitle().equals(filterPM.getTitle());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        try {
            return getJsonFilter();
        } catch (JSONException e) {
            return null;
        }
    }
}
