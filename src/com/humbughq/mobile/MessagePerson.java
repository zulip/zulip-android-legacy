package com.humbughq.mobile;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Mapping of messages to recipients
 * 
 */
@DatabaseTable(tableName = "recipients")
public class MessagePerson {
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(foreign = true)
    public Message msg;
    @DatabaseField(foreign = true)
    public Person recipient;

    public MessagePerson() {

    }

    public MessagePerson(Message message, Person recipient) {
        this.msg = message;
        this.recipient = recipient;
    }
}
