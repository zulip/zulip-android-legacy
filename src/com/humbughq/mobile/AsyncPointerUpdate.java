package com.humbughq.mobile;

class AsyncPointerUpdate extends HumbugAsyncPushTask {
    public AsyncPointerUpdate(ZulipApp app) {
        super(app);
    }

    public final void execute(int newPointer) {
        this.setProperty("pointer", Integer.toString(newPointer));
        execute("PUT", "v1/users/me/pointer");
    }
}
