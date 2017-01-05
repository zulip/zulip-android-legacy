package com.zulip.android.filters;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.stmt.Where;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;

import org.json.JSONArray;
import org.json.JSONException;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Filter all private messages involving this person
 */
public class NarrowFilterAllPMs implements NarrowFilter {
    private final Person person;
    private final String recipient;

    public static final Parcelable.Creator<NarrowFilterAllPMs> CREATOR = new Parcelable.Creator<NarrowFilterAllPMs>() {
        public NarrowFilterAllPMs createFromParcel(Parcel in) {

            return new NarrowFilterAllPMs(in.readString());
        }

        public NarrowFilterAllPMs[] newArray(int size) {
            return new NarrowFilterAllPMs[size];
        }
    };

    public NarrowFilterAllPMs(Person person) {
        this.person = person;
        this.recipient = Message.recipientList(new Person[]{person});
    }

    private NarrowFilterAllPMs(String recipient) {
        this.recipient = recipient;
        this.person = Person
                .getById(ZulipApp.get(), Integer.valueOf(recipient));
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(recipient);
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {

        // see if recipient matches any items in comma delimited string
        where.eq(Message.TYPE_FIELD, MessageType.PRIVATE_MESSAGE);

        return where;
    }

    @Override
    public boolean matches(Message msg) {
        return msg.getType() == MessageType.PRIVATE_MESSAGE;
    }

    @Override
    public String getTitle() {
        return "All private messages";
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
        JSONArray ret = new JSONArray();

        ret.put(new JSONArray(Arrays.asList("is", "private")));
        return ret.toString();
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
