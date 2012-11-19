package com.humbughq.android;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

public class Message {
    private String sender;
    private MessageType type;
    private String content;
    private String subject;
    private String senderEmail;
    private Date timestamp;
    private String[] recipients;
    private int id;
    private String your_email;

    public Message(HumbugActivity context, JSONObject message)
            throws JSONException {
        this.your_email = context.email;
        this.populate(message);
    }

    public Message(JSONObject message) throws JSONException {
        this.populate(message);
    }

    private String getNotYouRecipient(JSONObject other) {
        try {
            if (!other.getString("email").equals(this.your_email)) {
                return other.getString("full_name");
            } else {
                return this.getSender();
            }
        } catch (JSONException e) {
            Log.e("message", "Couldn't parse JSON sender list!");
            e.printStackTrace();
        }
        return null;
    }

    public void populate(JSONObject message) throws JSONException {
        this.setSender(message.getString("sender_full_name"));
        this.setSenderEmail(message.getString("sender_email"));
        if (message.getString("type").equals("stream")) {
            this.setType(MessageType.STREAM_MESSAGE);
            recipients = new String[1];
            recipients[0] = message.getString("display_recipient");
        } else if (message.getString("type").equals("huddle")) {
            this.setType(MessageType.HUDDLE_MESSAGE);
            JSONArray jsonRecipients = message
                    .getJSONArray("display_recipient");
            recipients = new String[jsonRecipients.length() - 1];

            for (int i = 0; i < jsonRecipients.length() - 1; i++) {
                recipients[i] = getNotYouRecipient(jsonRecipients
                        .getJSONObject(i));
            }
        } else if (message.getString("type").equals("personal")) {
            this.setType(MessageType.PERSONAL_MESSAGE);
            recipients = new String[1];
            recipients[0] = getNotYouRecipient(message
                    .getJSONObject("display_recipient"));
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

    public String getRecipient() {
        return TextUtils.join(", ", recipients);
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
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
}
