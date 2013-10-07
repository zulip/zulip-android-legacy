package com.humbughq.mobile;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.TextView;

class AsyncLogin extends HumbugAsyncPushTask {
    LoginActivity context;

    public AsyncLogin(LoginActivity loginActivity, String username,
            String password) {
        super(loginActivity.app);
        context = loginActivity;
        // Knowing your name would be nice, but we don't use it anywhere and
        // it's not returned by the API, so having it be null here is fine for
        // now.
        this.app.setEmail(username);
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
            errorText.post(new Runnable() {
                @Override
                public void run() {
                    errorText.setText("Incorrect username or password");
                }
            });
            this.cancel(true);
        } else {
            errorText.post(new Runnable() {
                @Override
                public void run() {
                    errorText.setText("Unknown error");
                }
            });
            // supermethod invokes cancel for us
            super.handleHTTPError(e);
        }
    }
}
