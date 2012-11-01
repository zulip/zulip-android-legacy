package com.humbughq.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences.Editor;
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
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.getString("result").equals("success")) {
                    this.context.api_key = obj.getString("api_key");
                    Log.i("login", "Logged in as " + this.context.api_key);

                    Editor ed = this.context.settings.edit();
                    ed.putString("email", this.context.email);
                    ed.putString("api_key", this.context.api_key);

                    ed.commit();

                    this.context.openLogin();
                    callback.onTaskComplete(result);
                    return;
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        TextView errorText = (TextView) this.context
                .findViewById(R.id.error_text);
        errorText.setText("Login failed");
    }
}