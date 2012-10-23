package com.humbughq.android;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

public class Message {
    public static final int STREAM_MESSAGE = 1;
    public static final int HUDDLE_MESSAGE = 2;
    public static final int PERSONAL_MESSAGE = 3;

    private String sender;
    private int type;
    private String content;
    private String subject;
    private String senderEmail;
    private Date curDateTime;
    private String[] recipients;

    public Message() {
        // Default constructor
    }

    public Message(JSONObject message) throws JSONException {
        this.populate(message);
    }

    public void populate(JSONObject message) throws JSONException {
        this.setSender(message.getString("sender_full_name"));
        this.setSenderEmail(message.getString("sender_email"));
        if (message.getString("type").equals("stream")) {
            this.setType(Message.STREAM_MESSAGE);
            recipients = new String[1];
            recipients[0] = message.getString("display_recipient");
        } else if (message.getString("type").equals("huddle")) {
            this.setType(Message.HUDDLE_MESSAGE);
            JSONArray jsonRecipients = message
                    .getJSONArray("display_recipient");
            recipients = new String[jsonRecipients.length()];

            for (int i = 0; i < jsonRecipients.length(); i++) {
                recipients[i] = jsonRecipients.getJSONObject(i).getString(
                        "short_name");
            }
        } else if (message.getString("type").equals("personal")) {
            this.setType(Message.PERSONAL_MESSAGE);
            recipients = new String[1];
            recipients[0] = message.getJSONObject("display_recipient")
                    .getString("short_name");
        }
        this.setContent(message.getString("content"));
        if (this.getType() == Message.STREAM_MESSAGE) {
            this.setSubject(message.getString("subject"));
        } else {
            this.setSubject(null);
        }

        this.setCurDateTime(new Date(message.getInt("timestamp")));
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public Date getCurDateTime() {
        return curDateTime;
    }

    public void setCurDateTime(Date curDateTime) {
        this.curDateTime = curDateTime;
    }
}
