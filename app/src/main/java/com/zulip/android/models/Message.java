package com.zulip.android.models;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.DatabaseTable;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.util.CustomHtmlToSpannedConverter;
import com.zulip.android.util.UrlHelper;
import com.zulip.android.util.ZLog;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@DatabaseTable(tableName = "messages")
public class Message {

    public static final String ID_FIELD = "id";
    private static final String SENDER_FIELD = "sender";
    public static final String TYPE_FIELD = "type";
    public static final String CONTENT_FIELD = "content";
    private static final String FORMATTED_CONTENT_FIELD = "formattedContent";
    public static final String SUBJECT_FIELD = "subject";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String RECIPIENTS_FIELD = "recipients";
    public static final String STREAM_FIELD = "stream";
    public static final String MESSAGE_READ_FIELD = "read";
    private static final String MESSAGE_EDITED = "MESSAGE_EDITED";
    private static final String MESSAGE_EDIT_DATE = "MESSAGE_EDIT_DATE";

    //region fields
    @SerializedName("recipient_id")
    private int recipientId;

    @SerializedName("sender_email")
    private String senderEmail;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("sender_full_name")
    private String senderFullName;

    @SerializedName("sender_domain")
    private String senderDomain;

    @SerializedName("gravatar_hash")
    private String gravatarHash;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("client")
    private String client;

    @SerializedName("content_type")
    private String contentType;

    @SerializedName("sender_short_name")
    private String senderShortName;

//    @SerializedName("type")
//    private String _internal_type;

    @SerializedName("subject_links")
    private List<?> subjectLinks;

    @DatabaseField(foreign = true, columnName = SENDER_FIELD, foreignAutoRefresh = true)
    private Person sender;

    @SerializedName("type")
    @DatabaseField(columnName = TYPE_FIELD)
    private MessageType type;

    @SerializedName("IGNORE_MASK_CONTENT")
    @DatabaseField(columnName = CONTENT_FIELD)
    private String content;

    @SerializedName("content")
    @DatabaseField(columnName = FORMATTED_CONTENT_FIELD)
    private String formattedContent;

    @SerializedName("subject")
    @DatabaseField(columnName = SUBJECT_FIELD)
    private String subject;

    @SerializedName("timestamp")
    @DatabaseField(columnName = TIMESTAMP_FIELD)
    private Date timestamp;

    @DatabaseField(columnName = RECIPIENTS_FIELD, index = true)
    private String recipients;

    private Person[] recipientsCache;

    @SerializedName("id")
    @DatabaseField(id = true, columnName = ID_FIELD)
    private int id;

    @DatabaseField(foreign = true, columnName = STREAM_FIELD, foreignAutoRefresh = true)
    private Stream stream;
    @DatabaseField(columnName = MESSAGE_READ_FIELD)
    private Boolean messageRead;

    @DatabaseField(columnDefinition = MESSAGE_EDITED)
    private Boolean hasBeenEdited;

    @DatabaseField(columnDefinition = MESSAGE_EDIT_DATE)
    private Date editDate;

    //IGNORE - This will always be empty due to persistence
    @SerializedName("edit_history")
    public List<MessageHistory> _history;
    //endregion

    /**
     * Construct an empty Message object.
     */
    protected Message() {

    }

    public Message(ZulipApp app) {
    }

    //region helpers
    /**
     * Populate a Message object based off a parsed JSON hash.
     *
     * @param app     The global ZulipApp.
     * @param message the JSON object as returned by the server.
     * @throws JSONException
     */
    public Message(ZulipApp app, JSONObject message,
                   Map<String, Person> personCache,
                   Map<String, Stream> streamCache) throws JSONException {
        this.setID(message.getInt("id"));
        this.setSender(Person.getOrUpdate(app,
                message.getString("sender_email"),
                message.getString("sender_full_name"),
                message.getString("avatar_url"), personCache));

        if (message.getString("type").equals("stream")) {
            this.setType(MessageType.STREAM_MESSAGE);

            String streamName = message.getString("display_recipient");

            Stream stream = null;
            if (streamCache != null) {
                stream = streamCache.get(streamName);
            }

            if (stream == null) {
                stream = Stream.getByName(app, streamName);
                if (streamCache != null) {
                    streamCache.put(streamName, stream);
                }
            }

            setStream(stream);
        } else if (message.getString("type").equals("private")) {
            this.setType(MessageType.PRIVATE_MESSAGE);

            JSONArray jsonRecipients = message
                    .getJSONArray("display_recipient");

            Person[] r = new Person[jsonRecipients.length()];
            for (int i = 0; i < jsonRecipients.length(); i++) {
                JSONObject obj = jsonRecipients.getJSONObject(i);
                Person person = Person.getOrUpdate(app, obj.getString("email"),
                        obj.getString("full_name"), null, personCache);
                r[i] = person;
            }
            setRecipients(recipientList(r));
        }

        String html = message.getString("content");
        this.setFormattedContent(html);

        // Use HTML to create formatted Spanned, then strip formatting
        Spanned formattedContent = formatContent(html, app);
        this.setContent(formattedContent.toString());

        if (this.getType() == MessageType.STREAM_MESSAGE) {
            this.setSubject(message.getString("subject"));
        } else {
            this.setSubject(null);
        }

        this.setTimestamp(new Date(message.getLong("timestamp") * 1000));
        this.setMessageRead(false);
    }

    public Boolean getMessageRead() {
        return messageRead;
    }

    public void setMessageRead(Boolean messageRead) {
        this.messageRead = messageRead;
    }

    public Message(ZulipApp app, JSONObject message) throws JSONException {
        this(app, message, null, null);
    }

    public static String recipientList(Person[] recipients) {
        Integer[] ids = new Integer[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            ids[i] = recipients[i].id;
        }
        Arrays.sort(ids);
        return TextUtils.join(",", ids);
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

        try {
            Person to = ZulipApp.get().getDao(Person.class, true).queryBuilder().where().eq(Person.EMAIL_FIELD, list[0].getEmail()).queryForFirst();;
            if(list.length == 1) {
                setRecipients(to.getId() + "");
                return;
            }
            Person from = ZulipApp.get().getDao(Person.class, true).queryBuilder().where().eq(Person.EMAIL_FIELD, list[1].getEmail()).queryForFirst();

            if(to == null && from != null) {
                setRecipients(""+ from.getId());
            }
            if(to != null && from == null) {
                setRecipients(to.getId() + "");
            }

            setRecipients(to.getId() + "," + from.getId());
            return;
        } catch (Exception e) {
            ZLog.logException(e);
        }

        this.recipients = (recipientId == 0 && senderId == 0) ? recipientList(list) : recipientId + "," + senderId;
    }

    /**
     * Convenience function to set the recipients without requiring the caller
     * to construct a full Person[] array.
     * <p/>
     * Do not call this method if you want to be able to get the recipient's
     * names for this message later; construct a Person[] array and use
     * setRecipient(Person[] recipients) instead.
     *
     * @param emails The emails of the recipients.
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
     * of all of the participants in the message.
     * <p/>
     * For MessageType.STREAM_MESSAGE, return the stream name instead.
     *
     * @return A String of the names of each Person in recipients[],
     * comma-separated, or the stream name.
     */
    public String getDisplayRecipient(ZulipApp app) {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getStream().getName();
        } else {
            Person[] people = this.getRecipients(app);
            ArrayList<String> names = new ArrayList<>();

            for (Person person : people) {
                if (person.id != app.getYou().id || people.length == 1) {
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
            return emailsMinusYou(Arrays.asList(people), app.getYou());
        }
    }

    public static String emailsMinusYou(List<Person> people, Person you) {
        ArrayList<String> names = new ArrayList<>();

        for (Person person : people) {
            if (person.id != you.id) {
                names.add(person.getEmail());
            }
        }
        return TextUtils.join(", ", names);
    }

    /**
     * Returns a Person array of the email addresses of the parties of the
     * message, the user excluded.
     *
     * @return said Person[].
     */
    public Person[] getPersonalReplyTo(ZulipApp app) {
        Person[] people = this.getRecipients(app);
        ArrayList<Person> names = new ArrayList<>();

        for (Person person : people) {
            if (person.id != app.getYou().id) {
                names.add(person);
            }
        }

        return people;
    }

    public static void createMessages(final ZulipApp app,
                                      final List<Message> messages) {
        try {
            TransactionManager.callInTransaction(app.getDatabaseHelper()
                    .getConnectionSource(), new Callable<Void>() {
                public Void call() throws Exception {
                    RuntimeExceptionDao<Message, Object> messageDao = app.getDao(Message.class);

                    for (Message m : messages) {
                        Person person = Person.getOrUpdate(app, m.getSenderEmail(), m.getSenderFullName(), m.getAvatarUrl());
                        m.setSender(person);
                        messageDao.createOrUpdate(m);
                    }
                    return null;
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void trim(final int olderThan, final ZulipApp app) {
        final RuntimeExceptionDao<Message, Integer> messageDao = app
                .<Message, Integer>getDao(Message.class);

        if (messageDao.countOf() <= olderThan) {
            return;
        }

        try {
            synchronized (app.updateRangeLock) {
                TransactionManager.callInTransaction(app.getDatabaseHelper()
                        .getConnectionSource(), new Callable<Void>() {
                    public Void call() throws Exception {

                        int topID = messageDao.queryBuilder()
                                .orderBy(Message.ID_FIELD, false)
                                .offset((long) olderThan).limit((long) 1)
                                .queryForFirst().getID();

                        DeleteBuilder<Message, Integer> messageDeleter = messageDao
                                .deleteBuilder();
                        messageDeleter.where().le(ID_FIELD, topID);
                        messageDeleter.delete();

                        MessageRange rng = MessageRange.getRangeContaining(
                                topID,
                                app.<MessageRange, Integer>getDao(MessageRange.class));
                        if (rng == null) {
                            Log.wtf("trim",
                                    "Message in database but not in range!");
                            return null;
                        }
                        if (rng.high == topID) {
                            rng.delete();
                        } else {
                            rng.low = topID + 1;
                            rng.update();
                        }
                        DeleteBuilder<MessageRange, Integer> dB2 = app
                                .<MessageRange, Integer>getDao(
                                        MessageRange.class).deleteBuilder();

                        dB2.where().le("high", topID);
                        dB2.delete();

                        return null;
                    }
                });
            }
        } catch (SQLException e) {
            ZLog.logException(e);
        }

    }

    public String concatStreamAndTopic() {
        return getStream().getId() + getSubject();
    }

    public String getIdForHolder() {
        if (this.getType() == MessageType.PRIVATE_MESSAGE) {
            return getRawRecipients();
        }
        return getStream().getId() + getSubject();
    }

    private static final HTMLSchema schema = new HTMLSchema();

    public Spanned getFormattedContent(ZulipApp app) {
        Spanned formattedMessage = formatContent(getFormattedContent(),
                app);

        while (formattedMessage.length() != 0
                && formattedMessage.charAt(formattedMessage.length() - 1) == '\n') {
            formattedMessage = (Spanned) formattedMessage.subSequence(0,
                    formattedMessage.length() - 2);
        }
        return formattedMessage;
    }

    /**
     * Copied from Html.fromHtml
     *
     * @param source HTML to be formatted
     * @param app {@link ZulipApp}
     * @return Span
     */
    public static Spanned formatContent(String source, final ZulipApp app) {
        final Context context = app.getApplicationContext();
        final float density = context.getResources().getDisplayMetrics().density;
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, schema);
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }

        Html.ImageGetter emojiGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                int lastIndex = -1;
                if (source != null) {
                    lastIndex = source.lastIndexOf('/');
                }
                if (lastIndex != -1) {
                    String filename = source.substring(lastIndex + 1);
                    try {
                        Drawable drawable = Drawable.createFromStream(context
                                        .getAssets().open("emoji/" + filename),
                                "emoji/" + filename);
                        if (drawable == null) {
                            Drawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
                            transparentDrawable.setBounds(new Rect(0, 0, 0, 0));
                            return transparentDrawable;
                        }

                        // scaling down by half to fit well in message
                        double scaleFactor = 0.5;
                        drawable.setBounds(0, 0,
                                (int) (drawable.getIntrinsicWidth()
                                        * scaleFactor * density),
                                (int) (drawable.getIntrinsicHeight()
                                        * scaleFactor * density));
                        return drawable;
                    } catch (IOException e) {
                        Log.e("RecyclerMessageAdapter", e.getMessage());
                    }
                }
                return null;
            }
        };

        CustomHtmlToSpannedConverter converter = new CustomHtmlToSpannedConverter(
                source, null, null, parser, emojiGetter, app.getServerURI(), context);

        return CustomHtmlToSpannedConverter.linkifySpanned(converter.convert(), Linkify.ALL);
    }
    //endregion

    //region model-getter-setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        if (subject != null && subject.equals("")) {
            // The empty string should be interpreted as "no topic"
            // i18n here will be sad
            this.subject = ZulipApp.get().getString(R.string.no_topic_in_message);
        } else {
            this.subject = subject;
        }
    }

    public String getContent() {
        if(content == null) {
            content = formatContent(getFormattedContent(), ZulipApp.get()).toString();
        }
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormattedContent() {
        return formattedContent;
    }

    private void setFormattedContent(String formattedContent) {
        this.formattedContent = formattedContent;
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

    private void setTimestamp(Date curDateTime) {
        this.timestamp = curDateTime;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public Stream getStream() {
        if(stream == null && getType() == MessageType.STREAM_MESSAGE) {
            stream = Stream.getByName(ZulipApp.get(), getRawRecipients());
        }
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public String getSenderDomain() {
        return senderDomain;
    }

    public String getGravatarHash() {
        return gravatarHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getClient() {
        return client;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSenderShortName() {
        return senderShortName;
    }

    public List<?> getSubjectLinks() {
        return subjectLinks;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getRecipients() {
        return recipients;
    }

    public Person[] getRecipientsCache() {
        return recipientsCache;
    }

    public int getId() {
        return id;
    }

    public static HTMLSchema getSchema() {
        return schema;
    }

    public void updateFromHistory(@NonNull MessageHistory history) {
        hasBeenEdited = true;
        editDate = history.date;
    }

    public boolean isHasBeenEdited() {
        return hasBeenEdited;
    }



    //endregion


    public static class ZulipDirectMessage extends Message {
        @SerializedName("display_recipient")
        private List<Person> displayRecipient;

        @Override
        public MessageType getType() {
            return super.getType() == null ? MessageType.PRIVATE_MESSAGE : super.getType();
        }

        @Override
        public Person[] getRecipients(ZulipApp app) {
            return getDisplayRecipient().toArray(new Person[getDisplayRecipient().size()]);
        }

        public List<Person> getDisplayRecipient() {
            return displayRecipient;
        }
    }

    public static class ZulipStreamMessage extends Message {
        @SerializedName("display_recipient")
        private String displayRecipient;

        @Override
        public MessageType getType() {
            return super.getType() == null ? MessageType.STREAM_MESSAGE : super.getType();
        }

        public String getDisplayRecipient() {
            return displayRecipient;
        }
    }

    public String extractImageUrl(ZulipApp zulipApp) {
        String match = "<img src=\"";
        int start = getFormattedContent().indexOf(match);

        if(start == -1){
            return null;
        }
        start += match.length();
        match = getFormattedContent().substring(start);
        if(match.indexOf("\"") == -1) {
            return null;
        }
        match = match.substring(0, match.indexOf("\""));

        if(match.indexOf("/") == 0) {
            return UrlHelper.addHost(match);
        }
        return match;
    }
}
