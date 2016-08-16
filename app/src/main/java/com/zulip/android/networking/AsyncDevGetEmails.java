package com.zulip.android.networking;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.activities.DevAuthActivity;
import com.zulip.android.activities.LoginActivity;
import com.zulip.android.util.ZLog;

import org.json.JSONException;
import org.json.JSONObject;


import static com.zulip.android.activities.DevAuthActivity.ADD_REALM_REQUEST;

/**
 * A background task which asynchronously fetches the Emails (Admins or Users) for the devAuthBackend
 * Mainly used Development builds.
 */
public class AsyncDevGetEmails extends ZulipAsyncPushTask {
    private static final String DISABLED = "dev_disabled";
    private Context context;
    public static final String SERVER_URL_JSON = "server_url";
    public static final String REALM_NAME_JSON = "realm_json";
    public static final String ADD_REALM_BOOLEAN_JSON = "add_realm_bool_json";
    public final static String EMAIL_JSON = "emails_json";
    private String serverURL;
    private String realmName;
    private boolean startedFromAddRealm;

    public AsyncDevGetEmails(LoginActivity loginActivity, String serverURL, String realmName, boolean startedFromAddRealm) {
        super((ZulipApp) loginActivity.getApplication());
        context = loginActivity;
        this.serverURL = serverURL;
        this.realmName = realmName;
        this.startedFromAddRealm = startedFromAddRealm;
        this.setServerURL(serverURL);
    }

    public final void execute() {
        execute("GET", "v1/dev_get_emails");
    }

    @Override
    protected void onPostExecute(String result) {
        try {
            JSONObject obj = new JSONObject(result);
            if (obj.getString("result").equals("success")) {
                Intent intent = new Intent(context, DevAuthActivity.class);
                intent.putExtra(EMAIL_JSON, result);
                intent.putExtra(REALM_NAME_JSON, realmName);
                intent.putExtra(SERVER_URL_JSON, serverURL);
                intent.putExtra(ADD_REALM_BOOLEAN_JSON, startedFromAddRealm);
                ((LoginActivity) context).startActivityForResult(intent, ADD_REALM_REQUEST);
            }
        } catch (JSONException e) {
            ZLog.logException(e);
        }
    }

    @Override
    protected void onCancelled(String result) {
        super.onCancelled(result);
        if (result == null) return;
        String message = context.getString(R.string.network_error);
        try {
            JSONObject obj = new JSONObject(result);
            message = obj.getString("msg");
        } catch (JSONException e1) {
            ZLog.logException(e1);
        }
        final String finalMessage = message;
        ((LoginActivity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, finalMessage, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }
}
