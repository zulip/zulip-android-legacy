package com.humbughq.mobile;

import java.sql.SQLException;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

public class NarrowFilterPM implements NarrowFilter {
    Person person;
    String recipientString;

    NarrowFilterPM(Person person) {
        this.person = person;
        this.recipientString = Message.recipientList(new Person[] { person,
                ZulipApp.get().you });
    }

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
        return person.getName();
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
        return person.getEmail();
    }
}
