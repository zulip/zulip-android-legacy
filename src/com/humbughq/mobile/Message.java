package com.humbughq.mobile;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "messages")
public class Message {

    public static final String SENDER_FIELD = "sender";
    public static final String TYPE_FIELD = "type";
    public static final String CONTENT_FIELD = "content";
    public static final String SUBJECT_FIELD = "subject";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String RECIPIENTS_FIELD = "recipients";
    public static final String STREAM_FIELD = "stream";

    @DatabaseField(foreign = true, columnName = SENDER_FIELD, foreignAutoRefresh = true)
    private Person sender;
    @DatabaseField(columnName = TYPE_FIELD)
    private MessageType type;
    @DatabaseField(columnName = CONTENT_FIELD)
    private String content;
    @DatabaseField(columnName = SUBJECT_FIELD)
    private String subject;
    @DatabaseField(columnName = TIMESTAMP_FIELD)
    private Date timestamp;
    @ForeignCollectionField(columnName = RECIPIENTS_FIELD, eager = true)
    private ForeignCollection<MessagePerson> recipients;
    @DatabaseField(id = true)
    private int id;
    @DatabaseField(foreign = true, columnName = STREAM_FIELD, foreignAutoRefresh = true)
    private Stream stream;

    /**
     * Construct an empty Message object.
     */
    protected Message() {

    }

    public Message(ZulipApp app) {
        try {
            recipients = app.getDatabaseHelper().getDao(Message.class)
                    .getEmptyForeignCollection(RECIPIENTS_FIELD);
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Convenience function to return either the Recipient specified or the
     * sender of the message as appropriate.
     * 
     * @param you
     * 
     * @param other
     *            a Recipient object you want to analyse
     * @return Either the specified Recipient's full name, or the sender's name
     *         if you are the Recipient.
     */
    private boolean getNotYouRecipient(Person you, JSONObject other) {
        try {
            if (you != null && !other.getString("email").equals(you.getEmail())) {
                return true;
            } else {
                return false;
            }
        } catch (JSONException e) {
            Log.e("message", "Couldn't parse JSON sender list!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Populate a Message object based off a parsed JSON hash.
     * 
     * @param app
     *            The global ZulipApp.
     * 
     * @param message
     *            the JSON object as returned by the server.
     * @throws JSONException
     */
    public Message(ZulipApp app, JSONObject message) throws JSONException {

        Person sender = new Person(message.getString("sender_full_name"),
                message.getString("sender_email"),
                message.getString("avatar_url"));
        try {
            app.getDao(Person.class).createOrUpdate(sender);
        } catch (SQLException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        this.setSender(sender);

        if (message.getString("type").equals("stream")) {
            this.setType(MessageType.STREAM_MESSAGE);

            setStream(Stream.getByName(app,
                    message.getString("display_recipient")));

        } else if (message.getString("type").equals("private")) {
            this.setType(MessageType.PRIVATE_MESSAGE);

            JSONArray jsonRecipients = message
                    .getJSONArray("display_recipient");
            int display_recipients = jsonRecipients.length() - 1;
            if (display_recipients == 0) {
                display_recipients = 1;
            }
            try {
                recipients = app.getDatabaseHelper().getDao(Message.class)
                        .getEmptyForeignCollection(RECIPIENTS_FIELD);
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            for (int i = 0, j = 0; i < jsonRecipients.length(); i++) {
                JSONObject obj = jsonRecipients.getJSONObject(i);

                if (getNotYouRecipient(app.you, obj) ||
                // If you sent a message to yourself, we still show your as the
                // other party.
                        jsonRecipients.length() == 1) {
                    recipients.add(new MessagePerson(this, new Person(obj
                            .getString("full_name"), obj.getString("email"))));
                    j++;
                }
            }
        }

        this.setContent(message.getString("content"));
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            this.setSubject(message.getString("subject"));
        } else {
            this.setSubject(null);
        }

        this.setTimestamp(new Date(message.getLong("timestamp") * 1000));
        this.setID(message.getInt("id"));
        try {
            app.getDatabaseHelper().getDao(Message.class)
                    .createIfNotExists(this);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(sender).append(type)
                .append(content).append(subject).append(timestamp).append(id)
                .append(stream).toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Message)) {
            return false;
        }
        Message msg = (Message) obj;

        return new EqualsBuilder().append(sender, msg.sender)
                .append(type, msg.type).append(content, msg.content)
                .append(subject, msg.subject).append(timestamp, msg.timestamp)
                .append(id, msg.id).append(stream, msg.stream).isEquals();
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType streamMessage) {
        this.type = streamMessage;
    }

    public void setRecipient(Collection<Person> recipients) {
        this.recipients.clear();
        for (Person recipient : recipients) {
            this.recipients.add(new MessagePerson(this, recipient));
        }
    }

    /**
     * Convenience function to set the recipients without requiring the caller
     * to construct a full Person[] array.
     * 
     * Do not call this method if you want to be able to get the recipient's
     * names for this message later; construct a Person[] array and use
     * setRecipient(Person[] recipients) instead.
     * 
     * @param emails
     *            The emails of the recipients.
     */
    public void setRecipient(String[] emails) {
        this.recipients.clear();
        for (String email : emails) {
            this.recipients
                    .add(new MessagePerson(this, new Person(null, email)));
        }
    }

    /**
     * Convenience function to set the recipient in case of a single recipient.
     * 
     * @param recipient
     *            The sole recipient of the message.
     */
    public void setRecipient(Person recipient) {
        assert (this.recipients.remove(null) == true);
        this.recipients.add(new MessagePerson(this, recipient));
    }

    /**
     * Constructs a pretty-printable-to-the-user string consisting of the names
     * of all of the participants in the message, minus you.
     * 
     * For MessageType.STREAM_MESSAGE, return the stream name instead.
     * 
     * @return A String of the names of each Person in recipients[],
     *         comma-separated, or the stream name.
     */
    public String getDisplayRecipient() {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getStream().getName();
        } else {
            MessagePerson[] recipientsArray = this.recipients
                    .toArray(new MessagePerson[0]);
            String[] names = new String[recipientsArray.length];

            for (int i = 0; i < recipientsArray.length; i++) {
                names[i] = recipientsArray[i].recipient.getName();
            }
            return TextUtils.join(", ", names);
        }
    }

    /**
     * Creates a comma-separated String of the email addressed of all the
     * recipients of the message, as would be suitable to place in the compose
     * box.
     * 
     * @return the aforementioned String.
     */
    public String getReplyTo() {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getSender().getEmail();
        }
        Person[] people = getPersonalReplyTo();
        String[] emails = new String[people.length];
        for (int i = 0; i < people.length; i++) {
            emails[i] = people[i].getEmail();
        }
        return TextUtils.join(", ", emails);
    }

    /**
     * Returns a Person array of the email addresses of the parties of the
     * message, the user excluded.
     * 
     * @return said Person[].
     */
    public Person[] getPersonalReplyTo() {
        MessagePerson[] messagePeople = this.recipients
                .toArray(new MessagePerson[0]);
        if (messagePeople.length == 0) {
            throw new WrongMessageType();
        }
        Person[] people = new Person[messagePeople.length];
        for (int i = 0; i < messagePeople.length; i++) {
            people[i] = messagePeople[i].recipient;
        }
        return people;
    }

    public String getFormattedTimestamp() {
        DateFormat format = new SimpleDateFormat("MMM dd HH:mm");
        format.setTimeZone(TimeZone.getDefault());
        return format.format(this.getTimestamp());
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Person getSender() {
        return sender;
    }

    public void setSender(Person sender) {
        this.sender = sender;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date curDateTime) {
        this.timestamp = curDateTime;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

}
