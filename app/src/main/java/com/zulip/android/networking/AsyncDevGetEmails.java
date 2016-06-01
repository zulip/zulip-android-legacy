package com.zulip.android.networking;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.activities.DevAuthActivity;
import com.zulip.android.activities.LoginActivity;
import com.zulip.android.util.ZLog;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;

public class AsyncDevGetEmails extends ZulipAsyncPushTask {
    private static final String DISABLED = "dev_disabled";
    Context context;
    public final static String EMAIL_JSON = "emails_json";

    public AsyncDevGetEmails(LoginActivity loginActivity) {
        super((ZulipApp) loginActivity.getApplication());
        context = loginActivity;
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
                context.startActivity(intent);
            }
        } catch (JSONException e) {
            ZLog.logException(e);
        }
    }


    @Override
    protected void handleHTTPError(final HttpResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
            String message = context.getString(R.string.network_error);
            try {
                JSONObject obj = new JSONObject(e.getMessage());
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
        } else {
            // supermethod invokes cancel for us
            super.handleHTTPError(e);
        }
    }
}
