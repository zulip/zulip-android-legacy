package com.humbughq.android;

import android.util.Log;
import android.widget.TextView;

class AsyncLogin extends HumbugAsyncPushTask {

    public AsyncLogin(HumbugActivity humbugActivity, String username,
            String password) {
        super(humbugActivity);
        context = humbugActivity;
        this.context.email = username;
        this.setProperty("username", username);
        this.setProperty("password", password);
    }

    public final void execute() {
        execute("api/v1/fetch_api_key");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            this.context.api_key = result.toString();
            Log.i("login", "Logged in as " + this.context.api_key);

            this.context.openLogin();
        } else {
            TextView errorText = (TextView) this.context
                    .findViewById(R.id.error_text);
            errorText.setText("Login failed");
        }
    }
}