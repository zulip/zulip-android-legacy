package com.zulip.android;

import org.json.JSONArray;

public class AsyncSend extends ZulipAsyncPushTask {

    /**
     * Initialise an AsyncSend task to send a specific message.
     * 
     * @param humbugActivity
     *            The calling Activity
     * @param msg
     *            The message to send.
     */
    public AsyncSend(ZulipActivity humbugActivity, Message msg) {
        super(humbugActivity.app);
        this.setProperty("type", msg.getType().toString());
        if (msg.getType() == MessageType.STREAM_MESSAGE) {
            this.setProperty("to", msg.getStream().getName());
        } else {
            JSONArray arr = new JSONArray();
            for (Person recipient : msg.getPersonalReplyTo(humbugActivity.app)) {
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
