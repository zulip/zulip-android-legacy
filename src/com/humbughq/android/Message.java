package com.humbughq.android;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

public class Message {

    private Person sender;
    private MessageType type;
    private String content;
    private String subject;
    private Date timestamp;
    private Person[] recipients;
    private int id;
    private String stream;
    private Person you;

    /**
     * Construct a new Message from JSON returned by the Humbug server.
     * 
     * @param you
     *            A Person object corresponding to the user's name and email.
     * @param message
     *            The JSON object parsed from the server's output
     * @throws JSONException
     *             Thrown if the JSON provided is malformed.
     */
    public Message(Person you, JSONObject message) throws JSONException {
        this.you = you;
        this.populate(message);
    }

    /**
     * Construct a new Message from JSON returned by the Humbug server.
     * 
     * This method operates without information about the user, so some
     * automatic features like excluding the user from the recipient list are
     * not performed.
     * 
     * @param message
     *            The JSON object parsed from the server's output
     * @throws JSONException
     *             Thrown if the JSON provided is malformed.
     */
    public Message(JSONObject message) throws JSONException {
        this.populate(message);
    }

    public Message() {
        // Dummy empty constructor
    }

    /**
     * Convenience function to return either the Recipient specified or the
     * sender of the message as appropriate.
     * 
     * @param other
     *            a Recipient object you want to analyse
     * @return Either the specified Recipient's full name, or the sender's name
     *         if you are the Recipient.
     */
    private boolean getNotYouRecipient(JSONObject other) {
        try {
            if (!other.getString("email").equals(this.you.getEmail())) {
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
     * @param message
     *            the JSON object as returned by the server.
     * @throws JSONException
     */
    public void populate(JSONObject message) throws JSONException {
        this.setSender(new Person(message.getString("sender_full_name"),
                message.getString("sender_email")));
        if (message.getString("type").equals("stream")) {
            this.setType(MessageType.STREAM_MESSAGE);
            setStream(message.getString("display_recipient"));
        } else if (message.getString("type").equals("huddle")) {
            this.setType(MessageType.HUDDLE_MESSAGE);
            JSONArray jsonRecipients = message
                    .getJSONArray("display_recipient");
            recipients = new Person[jsonRecipients.length() - 1];
            if (message.getInt("id") == 303377) {
                Log.i("ho", "fuck");
            }
            for (int i = 0, j = 0; i < jsonRecipients.length(); i++) {
                JSONObject obj = jsonRecipients.getJSONObject(i);
                if (getNotYouRecipient(obj)) {
                    recipients[j] = new Person(obj.getString("full_name"),
                            obj.getString("email"));
                    j++;
                }
            }
        } else if (message.getString("type").equals("personal")) {
            this.setType(MessageType.PERSONAL_MESSAGE);
            recipients = new Person[1];
            JSONObject obj = message.getJSONObject("display_recipient");
            recipients[0] = new Person(obj.getString("full_name"),
                    obj.getString("email"));
        }
        this.setContent(message.getString("content"));
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            this.setSubject(message.getString("subject"));
        } else {
            this.setSubject(null);
        }

        this.setTimestamp(new Date(message.getInt("timestamp")));
        this.setID(message.getInt("id"));
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType streamMessage) {
        this.type = streamMessage;
    }

    public void setRecipient(Person[] recipients) {
        this.recipients = recipients;
    }

    public void setRecipient(String[] emails) {
        this.recipients = new Person[emails.length];
        for (int i = 0; i < emails.length; i++) {
            this.recipients[i] = new Person(null, emails[i]);
        }
    }

    /**
     * Convenience function to set the recipient in case of a single recipient.
     * 
     * @param recipient
     *            The sole recipient of the message.
     */
    public void setRecipient(Person recipient) {
        Person[] recipients = new Person[1];
        recipients[0] = recipient;
        this.recipients = recipients;
    }

    public String getDisplayRecipient() {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getStream();
        } else {
            String[] names = new String[this.recipients.length];

            for (int i = 0; i < this.recipients.length; i++) {
                names[i] = recipients[i].getName();
            }
            return TextUtils.join(", ", names);
        }
    }

    public String getReplyTo() {
        if (this.getType() == MessageType.STREAM_MESSAGE) {
            return this.getSender().getEmail();
        }
        return TextUtils.join(", ", getReplyToArray());
    }

    public String[] getReplyToArray() {
        String[] emails = new String[this.recipients.length];

        for (int i = 0; i < this.recipients.length; i++) {
            emails[i] = recipients[i].getEmail();
        }
        return emails;
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

}
