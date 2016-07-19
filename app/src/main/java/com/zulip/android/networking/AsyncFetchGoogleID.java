package com.zulip.android.networking;

import com.zulip.android.ZulipApp;

public class AsyncFetchGoogleID extends ZulipAsyncPushTask {

    public AsyncFetchGoogleID(ZulipApp app) {
        super(app);
    }

    public final void execute() {
        execute("GET", "v1/fetch_google_client_id");
    }
}
