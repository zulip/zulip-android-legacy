package com.zulip.android.networking;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.zulip.android.activities.DevAuthActivity;
import com.zulip.android.activities.LoginActivity;
import com.zulip.android.util.ZLog;
import com.zulip.android.ZulipApp;

/**
 * A background task which is used to authenticate from the server.
 * Currently this files handles EmailBackend, DevAuthBackend and GoogleMobileOauth2Backend.
 */
public class AsyncLogin extends ZulipAsyncPushTask {
    private static final String UNREGISTERED = "unregistered";
    private static final String DISABLED = "disabled";

    private Activity activity;
    private boolean devServer = true; //If this is a DevAuthBackend server!
    private LoginActivity context;
    private boolean userDefinitelyInvalid = false;

    /**
     * @param activity Reference to the activity from this is called mainly {@link LoginActivity} and {@link DevAuthActivity}
     * @param username Stores the E-Mail of the user or a string "google-oauth2-token" if this is for Google Authentication
     * @param password Stores the password if EmailBackend, ID Token if GoogleMobileOauth2Backend, and blank if DevAuthBackend
     */
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
    protected void onCancelled(String result) {
        super.onCancelled(result);
        String message = "Unknown authentication error.";
        try {
            JSONObject obj = new JSONObject(result);
            message = obj.getString("msg");
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
    }
}
