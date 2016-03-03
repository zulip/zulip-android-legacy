package com.zulip.android;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

class AsyncLogin extends ZulipAsyncPushTask {
    public static final String UNREGISTERED = "unregistered";
    public static final String DISABLED = "disabled";

    LoginActivity context;
    boolean userDefinitelyInvalid = false;

    public AsyncLogin(LoginActivity loginActivity, String username,
            String password) {
        super(loginActivity.app);
        context = loginActivity;
        if (username.contains("@") == true) {
            // @-less usernames are used as indicating special cases, for
            // example in OAuth2 authentication
            this.app.setEmail(username);
        }
        this.setProperty("username", username);
        this.setProperty("password", password);
    }

    public final void execute() {
        execute("POST", "v1/fetch_api_key");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.getString("result").equals("success")) {
                    this.app.setLoggedInApiKey(obj.getString("api_key"));
                    this.context.openHome();
                    callback.onTaskComplete(result);

                    return;
                }
            } catch (JSONException e) {
                ZLog.logException(e);
            }
        }
        Toast.makeText(context, "Unknown error", Toast.LENGTH_LONG).show();
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
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, finalMessage, Toast.LENGTH_LONG)
                            .show();
                }
            });
            this.cancel(true);
        } else {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Network error", Toast.LENGTH_LONG)
                            .show();
                }
            });
            // supermethod invokes cancel for us
            super.handleHTTPError(e);
        }
    }
}
