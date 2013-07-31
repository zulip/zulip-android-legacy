package com.humbughq.mobile;

import org.json.JSONArray;

public class AsyncSend extends HumbugAsyncPushTask {

    /**
     * Initialise an AsyncSend task to send a specific message.
     * 
     * @param humbugActivity
     *            The calling Activity
     * @param msg
     *            The message to send.
     */
    public AsyncSend(HumbugActivity humbugActivity, Message msg) {
        super(humbugActivity);
        this.setProperty("type", msg.getType().toString());
        if (msg.getType() == MessageType.STREAM_MESSAGE) {
            this.setProperty("to", msg.getStream());
        } else {
            JSONArray arr = new JSONArray();
            for (Person recipient : msg.getPersonalReplyTo()) {
                arr.put(recipient.getEmail());
            }
            this.setProperty("to", arr.toString());
        }
        this.setProperty("stream", msg.getSubject());
        this.setProperty("subject", msg.getSubject());
        this.setProperty("content", msg.getContent());
    }

    public final void execute() {
        execute("POST", "v1/messages");
    }

}
