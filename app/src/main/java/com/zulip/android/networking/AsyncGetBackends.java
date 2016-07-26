package com.zulip.android.networking;

import com.zulip.android.ZulipApp;

public class AsyncGetBackends extends ZulipAsyncPushTask {

    public AsyncGetBackends(ZulipApp app) {
        super(app);
    }

    //Json Format - {"msg":"","password":false,"google":false,"result":"success","dev":true}
    public final void execute() {
        execute("POST", "v1/get_auth_backends");
    }
}
