package com.zulip.android.networking;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.zulip.android.activities.DevAuthActivity;
import com.zulip.android.activities.LoginActivity;
import com.zulip.android.util.ZLog;
import com.zulip.android.ZulipApp;

public class AsyncLogin extends ZulipAsyncPushTask {
    public static final String UNREGISTERED = "unregistered";
    public static final String DISABLED = "disabled";

    Activity activity;
    boolean userDefinitelyInvalid = false;
    boolean devServer = true; //If this is a DevAuthBackend server!

    public AsyncLogin(Activity activity, String username, String password, boolean devServer) {
        super(ZulipApp.get());
        this.activity = activity;
        if (username.contains("@")) {
            // @-less usernames are used as indicating special cases, for
            // example in OAuth2 authentication
            this.app.setEmail(username);
        }
        this.setProperty("username", username);
        if (!devServer) {
            this.setProperty("password", password);
            this.devServer = false;
        }
    }

    public final void execute() {
        if (devServer) execute("POST", "v1/dev_fetch_api_key");
        else execute("POST", "v1/fetch_api_key");
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.getString("result").equals("success")) {
                    this.app.setLoggedInApiKey(obj.getString("api_key"));
                        if (devServer) ((DevAuthActivity) activity).openHome();
                        else ((LoginActivity) activity).openHome();
                    callback.onTaskComplete(result, obj);

                    return;
                }
            } catch (JSONException e) {
                ZLog.logException(e);
            }
        }
        Toast.makeText(activity, "Unknown error", Toast.LENGTH_LONG).show();
        Log.wtf("login", "We shouldn't have gotten this far.");
    }

    @Override
    protected void handleHTTPError(final HttpResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
            String message = "Unknown authentication error.";
            try {
                JSONObject obj = new JSONObject(e.getMessage());
                String reason = obj.getString("reason");
                message = obj.getString("msg");
                /*
                 * If you're disabled or unregistered, your credentials were
                 * valid but you are not able to use the service.
                 * 
                 * This is useful in the context of Google Apps login where we
                 * want to differentiate between "bad credentials" and
                 * "not permitted by policy". So, an authentication vs.
                 * authorization thing.
                 */
                userDefinitelyInvalid = reason.equals(AsyncLogin.DISABLED)
                        || reason.equals(AsyncLogin.UNREGISTERED);
            } catch (JSONException e1) {
                ZLog.logException(e1);
            }
            final String finalMessage = message;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, finalMessage, Toast.LENGTH_LONG)
                            .show();
                }
            });
            this.cancel(true);
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Network error", Toast.LENGTH_LONG)
                            .show();
                }
            });
            // supermethod invokes cancel for us
            super.handleHTTPError(e);
        }
    }
}
