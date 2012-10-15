package com.humbughq.android;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    public static final int STREAM_MESSAGE = 1;
    public static final int HUDDLE_MESSAGE = 2;
    
    private String sender;
    private String display_recipient;
    private int type;
    private String content;
    private String subject;
    
    public Message (JSONObject message) throws JSONException {
        this.setSender(message.getString("sender_name"));
        this.setDisplayRecipient(message.getString("display_recipient"));
        if (message.getString("type").equals("stream")) {
            this.setType(Message.STREAM_MESSAGE);
        } else if (message.getString("type").equals("huddle")) {
            this.setType(Message.HUDDLE_MESSAGE);
        }
        this.setContent(message.getString("content"));
        if (this.getType() == Message.STREAM_MESSAGE) {
            this.setSubject(message.getString("subject"));
        } else {
            this.setSubject(null);
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDisplayRecipient() {
        return display_recipient;
    }

    public void setDisplayRecipient(String display_recipient) {
        this.display_recipient = display_recipient;
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
}
