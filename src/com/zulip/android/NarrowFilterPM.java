package com.zulip.android;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

public class NarrowFilterPM implements NarrowFilter {
    List<Person> people;
    String recipientString;

    NarrowFilterPM(List<Person> people) {
        this.people = people;
        this.recipientString = Message.recipientList(people
                .toArray(new Person[0]));
    }

    private NarrowFilterPM(String recipientString) {
        this.recipientString = recipientString;
        this.people = new ArrayList<Person>();
        for (String id : this.recipientString.split(",")) {
            this.people
                    .add(Person.getById(ZulipApp.get(), Integer.valueOf(id)));
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.recipientString);
    }

    public static final Parcelable.Creator<NarrowFilterPM> CREATOR = new Parcelable.Creator<NarrowFilterPM>() {
        public NarrowFilterPM createFromParcel(Parcel in) {
            return new NarrowFilterPM(in.readString());
        }

        public NarrowFilterPM[] newArray(int size) {
            return new NarrowFilterPM[size];
        }
    };

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {

        where.eq(Message.RECIPIENTS_FIELD, new SelectArg(recipientString));
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        if (msg.getType() == MessageType.PRIVATE_MESSAGE) {
            return msg.getRawRecipients().equals(recipientString);
        }
        return false;
    }

    @Override
    public String getTitle() {
        ArrayList<String> names = new ArrayList<String>();
        for (Person person : people) {
            if (!person.equals(ZulipApp.get().you)) {
                names.add(person.getName());
            }
        }
        return TextUtils.join(", ", names);
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
        return Message.emailsMinusYou(people, ZulipApp.get().you);
    }

    @Override
    public String getJsonFilter() throws JSONException {
        ArrayList<String> emails = new ArrayList<String>();
        for (Person person : this.people) {
            if (!person.equals(ZulipApp.get().you)) {
                emails.add(person.getEmail());
            }
        }
        return (new JSONArray()).put(
                new JSONArray(Arrays.asList("pm-with",
                        TextUtils.join(",", emails)))).toString();
    }
}
