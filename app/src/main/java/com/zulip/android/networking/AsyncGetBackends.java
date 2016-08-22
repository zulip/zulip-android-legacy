package com.zulip.android.networking;

import com.zulip.android.ZulipApp;

/**
 * A background task which asynchronously fetches the supported Backend's by the server.
 * Used by the new Login Flow, which only shows the Views to login from the supported Authentications.
 */
public class AsyncGetBackends extends ZulipAsyncPushTask {

    public AsyncGetBackends(ZulipApp app) {
        super(app);
    }

    //Json Format - {"msg":"","password":false,"google":false,"result":"success","dev":true}
    public final void execute() {
        execute("POST", "v1/get_auth_backends");
    }
}
