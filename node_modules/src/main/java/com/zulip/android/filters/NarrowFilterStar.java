package com.zulip.android.filters;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;

import org.json.JSONException;

import java.sql.SQLException;

public class NarrowFilterStar implements NarrowFilter {

    public static final Parcelable.Creator<NarrowFilterStar> CREATOR = new Parcelable.Creator<NarrowFilterStar>() {
        public NarrowFilterStar createFromParcel(Parcel in) {
            return new NarrowFilterStar();
        }

        public NarrowFilterStar[] newArray(int size) {
            return new NarrowFilterStar[size];
        }
    };

    private boolean isStarred;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        where.eq(Message.MESSAGE_STAR_FIELD, new SelectArg(isStarred));
        return where;
    }

    @Override
    public String toString() {
        try {
            return getJsonFilter();
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean matches(Message msg) {
        return (msg.getFlags() != null && msg.getFlags().contains("starred")) ||
                (msg.getMessageStar());
    }

    @Override
    public String getTitle() {
        return ZulipApp.get().getString(R.string.starred_messages);
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
    public boolean equals(NarrowFilter filter) {
        return filter instanceof NarrowFilterStar;
    }
}
