package com.zulip.android;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.TextView;

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
        TextView errorText = (TextView) this.context
                .findViewById(R.id.error_text);
        errorText.setText("Unknown error");
        Log.wtf("login", "We shouldn't have gotten this far.");
    }

    @Override
    protected void handleHTTPError(final HttpResponseException e) {
        final TextView errorText = (TextView) this.context
                .findViewById(R.id.error_text);
        if (e.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
            String message = "Unknown authentication error.";
            try {
                JSONObject obj = new JSONObject(e.getMessage());
                String reason = obj.getString("reason");
                message = obj.getString("msg");
                userDefinitelyInvalid = reason.equals(AsyncLogin.DISABLED)
                        || reason.equals(AsyncLogin.UNREGISTERED);
            } catch (JSONException e1) {
                ZLog.logException(e1);
            }
            final String finalMessage = message;
            errorText.post(new Runnable() {
                @Override
                public void run() {
                    errorText.setText(finalMessage);
                }
            });
            this.cancel(true);
        } else {
            errorText.post(new Runnable() {
                @Override
                public void run() {
                    errorText.setText("Network error");
                }
            });
            // supermethod invokes cancel for us
            super.handleHTTPError(e);
        }
    }
}
