package com.zulip.android.networking;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.zulip.android.activities.DevAuthActivity;
import com.zulip.android.activities.LoginActivity;
import com.zulip.android.activities.ZulipActivity;
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
    private Activity context;
    private boolean userDefinitelyInvalid = false;
    boolean startedFromAddRealm = false;
    private String realmName;
    private String username;
    private String serverURL;
    LoginInterface loginInterface;

    /**
     * @param activity Reference to the activity from this is called mainly {@link LoginActivity} and {@link DevAuthActivity}
     * @param username Stores the E-Mail of the user or a string "google-oauth2-token" if this is for Google Authentication
     * @param password Stores the password if EmailBackend, ID Token if GoogleMobileOauth2Backend, and blank if DevAuthBackend
     */
    public AsyncLogin(Activity loginActivity, String username, String password, String realmName, boolean startedFromAddRealm, String serverURL, boolean devServer) {
        super(ZulipApp.get());
        this.startedFromAddRealm = startedFromAddRealm;
        context = loginActivity;
        this.username = username;
        if (username.contains("@") && !startedFromAddRealm) {
            // @-less usernames are used as indicating special cases, for
            // example in OAuth2 authentication
            this.app.setEmail(username);
        }
        this.setProperty("username", username);
        if (!devServer) {
            this.setProperty("password", password);
            this.devServer = false;
        }
        this.setProperty("password", password);
        this.realmName = realmName;
        this.setServerURL(serverURL);
        this.serverURL = serverURL;
        loginInterface = (LoginInterface) context;
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
                    if (startedFromAddRealm) {
                        loginThroughAddRealm(obj);
                        callback.onTaskComplete(result, obj);
                    } else {
                        this.app.setServerURL(serverURL);
                        this.app.setLoggedInApiKey(obj.getString("api_key"));
                        ZulipApp.get().saveServerName(realmName);
                        loginInterface.openHome();
                    }
                    return;
                }
            } catch (JSONException e) {
                ZLog.logException(e);
            }
        }
        Toast.makeText(activity, "Unknown error", Toast.LENGTH_LONG).show();
        Log.wtf("login", "We shouldn't have gotten this far.");
    }

    private void loginThroughAddRealm(JSONObject jsonObject) {
        try {
            Intent intent = new Intent(context, ZulipActivity.class);
            intent.putExtra("realmName", realmName);
            intent.putExtra("api_key", jsonObject.getString("api_key"));
            intent.putExtra("email", username);
            intent.putExtra("serverURL", serverURL);
            context.setResult(Activity.RESULT_OK, intent);
            context.finish();
        } catch (JSONException e) {
            ZLog.logException(e);
        }
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
