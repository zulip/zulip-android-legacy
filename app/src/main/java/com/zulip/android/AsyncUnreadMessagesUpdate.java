package com.zulip.android;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class AsyncUnreadMessagesUpdate extends ZulipAsyncPushTask {

    public AsyncUnreadMessagesUpdate(ZulipApp app) {
        super(app);

    }

    public final void execute() {
        ArrayList<Integer> messageIds = new ArrayList<Integer>();
        while (true) {
            Integer item = app.unreadMessageQueue.poll();
            if (item == null) {
                break;
            } else {
                messageIds.add(item);
            }
        }

        if (messageIds.size() > 0) {
            setProperty("messages", "[" + TextUtils.join(",", messageIds) + "]");
            setProperty("flag", "read");
            setProperty("op", "add");

            execute("POST", "v1/messages/flags");
        }
    }
}
