package com.humbughq.mobile;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.j256.ormlite.field.DatabaseField;
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
    @DatabaseField(columnName = RECIPIENTS_FIELD, index = true)
    private String recipients;
    private Person[] recipientsCache;
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
    }

    static String recipientList(Person[] recipients) {
        Integer[] ids = new Integer[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            ids[i] = recipients[i].id;
        }
        Arrays.sort(ids);
        return TextUtils.join(",", ids);
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
        this.setID(message.getInt("id"));
        this.setSender(Person.getOrUpdate(app,
                message.getString("sender_email"),
                message.getString("sender_full_name"),
                message.getString("avatar_url")));

        if (message.getString("type").equals("stream")) {
            this.setType(MessageType.STREAM_MESSAGE);

            setStream(Stream.getByName(app,
                    message.getString("display_recipient")));

        } else if (message.getString("type").equals("private")) {
            this.setType(MessageType.PRIVATE_MESSAGE);

            JSONArray jsonRecipients = message
                    .getJSONArray("display_recipient");

            Person[] r = new Person[jsonRecipients.length()];
            for (int i = 0; i < jsonRecipients.length(); i++) {
                JSONObject obj = jsonRecipients.getJSONObject(i);
                Person person = Person.getOrUpdate(app, obj.getString("email"),
                        obj.getString("full_name"), null);
                r[i] = person;
            }
            recipients = recipientList(r);
        }

        this.setContent(message.getString("content"));
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            this.setSubject(message.getString("subject"));
        } else {
            this.setSubject(null);
        }

        this.setTimestamp(new Date(message.getLong("timestamp") * 1000));
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

    public String getRawRecipients() {
        return recipients;
    }

    public Person[] getRecipients(ZulipApp app) {
        if (recipientsCache == null) {
            String[] ids = TextUtils.split(this.recipients, ",");
            recipientsCache = new Person[ids.length];
            for (int i = 0; i < ids.length; i++) {
                recipientsCache[i] = Person.getById(app,
                        Integer.parseInt(ids[i]));
            }
        }
        return recipientsCache;
    }

    public void setRecipients(Person[] list) {
        this.recipientsCache = list;
        this.recipients = recipientList(list);
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
        Person[] r = new Person[emails.length];
        for (int i = 0; i < emails.length; i++) {
            r[i] = new Person(null, emails[i]);
        }
        setRecipients(r);
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
    public String getDisplayRecipient(ZulipApp app) {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getStream().getName();
        } else {
            Person[] people = this.getRecipients(app);
            ArrayList<String> names = new ArrayList<String>();

            for (Person person : people) {
                if (person.id != app.you.id) {
                    names.add(person.getName());
                }
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
    public String getReplyTo(ZulipApp app) {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getSender().getEmail();
        } else {
            Person[] people = this.getRecipients(app);
            ArrayList<String> names = new ArrayList<String>();

            for (Person person : people) {
                if (person.id != app.you.id) {
                    names.add(person.getEmail());
                }
            }
            return TextUtils.join(", ", names);
        }
    }

    /**
     * Returns a Person array of the email addresses of the parties of the
     * message, the user excluded.
     * 
     * @return said Person[].
     */
    public Person[] getPersonalReplyTo(ZulipApp app) {
        Person[] people = this.getRecipients(app);
        ArrayList<Person> names = new ArrayList<Person>();

        for (Person person : people) {
            if (person.id != app.you.id) {
                names.add(person);
            }
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
