package com.humbughq.android;

public class AsyncSend extends HumbugAsyncPushTask {

    public AsyncSend(HumbugActivity humbugActivity, Message msg) {
        super(humbugActivity);
        this.setProperty("type", msg.getType().toString());
        this.setProperty("to", msg.getRecipient());
        this.setProperty("subject", msg.getSubject());
    }

    public final void execute() {
        execute("api/v1/send_message");
    }

}
