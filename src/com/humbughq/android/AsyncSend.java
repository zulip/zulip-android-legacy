package com.humbughq.android;

public class AsyncSend extends HumbugAsyncPushTask {

    public AsyncSend(HumbugActivity humbugActivity, Message msg) {
        super(humbugActivity);
        this.setProperty("type", msg.getType().toString());
        this.setProperty("to", msg.getRecipient());
        this.setProperty("stream", msg.getSubject());
        this.setProperty("subject", msg.getSubject());
        this.setProperty("content", msg.getContent());
    }

    public final void execute() {
        execute("api/v1/send_message");
    }

}
