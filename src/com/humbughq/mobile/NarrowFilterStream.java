package com.humbughq.mobile;

import java.sql.SQLException;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

public class NarrowFilterStream implements NarrowFilter {
    Stream stream;

    public NarrowFilterStream(Stream stream) {
        this.stream = stream;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(stream.getName());
    }

    public static final Parcelable.Creator<NarrowFilterStream> CREATOR = new Parcelable.Creator<NarrowFilterStream>() {
        public NarrowFilterStream createFromParcel(Parcel in) {
            return new NarrowFilterStream(Stream.getByName(ZulipApp.get(),
                    in.readString()));
        }

        public NarrowFilterStream[] newArray(int size) {
            return new NarrowFilterStream[size];
        }
    };

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        where.eq(Message.STREAM_FIELD, new SelectArg(stream));
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
        return null;
    }

    @Override
    public Stream getComposeStream() {
        return stream;
    }

    @Override
    public String getComposePMRecipient() {
        return null;
    }
}
