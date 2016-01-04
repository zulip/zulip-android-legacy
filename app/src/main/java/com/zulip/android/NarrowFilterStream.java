package com.zulip.android;

import java.sql.SQLException;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

public class NarrowFilterStream implements NarrowFilter {
    Stream stream;
    String subject;

    public NarrowFilterStream(Stream stream, String subject) {
        this.stream = stream;
        this.subject = subject;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        String[] pair = { stream.getName(), subject };
        dest.writeStringArray(pair);
    }

    public static final Parcelable.Creator<NarrowFilterStream> CREATOR = new Parcelable.Creator<NarrowFilterStream>() {
        public NarrowFilterStream createFromParcel(Parcel in) {
            String[] pair = in.createStringArray();
            return new NarrowFilterStream(Stream.getByName(ZulipApp.get(),
                    pair[0]), pair[1]);
        }

        public NarrowFilterStream[] newArray(int size) {
            return new NarrowFilterStream[size];
        }
    };

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        where.eq(Message.STREAM_FIELD, new SelectArg(stream));
        if (subject != null) {
            where.and().eq(Message.SUBJECT_FIELD, new SelectArg(subject));
        }
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        return msg.getType() == MessageType.STREAM_MESSAGE
                && msg.getStream().equals(stream);
    }

    @Override
    public String getTitle() {
        return stream.getName();
    }

    @Override
    public String getSubtitle() {
        return subject;
    }

    @Override
    public Stream getComposeStream() {
        return stream;
    }

    @Override
    public String getComposePMRecipient() {
        return null;
    }

    @Override
    public String getJsonFilter() throws JSONException {
        JSONArray filter = new JSONArray();
        filter.put(new JSONArray(Arrays.asList("stream", this.stream.getName())));
        if (subject != null) {
            filter.put(new JSONArray(Arrays.asList("topic", this.subject)));
        }
        return filter.toString();
    }
}
