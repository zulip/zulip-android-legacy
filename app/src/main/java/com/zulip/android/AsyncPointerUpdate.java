package com.zulip.android;

class AsyncPointerUpdate extends ZulipAsyncPushTask {
    public AsyncPointerUpdate(ZulipApp app) {
        super(app);
    }

    public final void execute(int newPointer) {
        this.setProperty("pointer", Integer.toString(newPointer));
        execute("PUT", "v1/users/me/pointer");
    }
}
