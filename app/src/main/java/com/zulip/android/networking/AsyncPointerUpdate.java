package com.zulip.android.networking;

import com.zulip.android.ZulipApp;

public class AsyncPointerUpdate extends ZulipAsyncPushTask {
    public AsyncPointerUpdate(ZulipApp app) {
        super(app);
    }

    public final void execute(int newPointer) {
        this.setProperty("pointer", Integer.toString(newPointer));
        execute("PUT", "/v1/users/me/pointer");
    }
}
