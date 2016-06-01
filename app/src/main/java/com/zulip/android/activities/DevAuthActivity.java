package com.zulip.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.networking.AsyncDevGetEmails;
import com.zulip.android.networking.AsyncLogin;
import com.zulip.android.util.ZLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DevAuthActivity extends Activity {
    private ProgressDialog connectionProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_auth);
        String json = getIntent().getStringExtra(AsyncDevGetEmails.EMAIL_JSON);
        List<String> emails = new ArrayList<>();
        int directAdminSize = 1;
        connectionProgressDialog = new ProgressDialog(this);
        connectionProgressDialog.setMessage(getString(R.string.signing_in));

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("direct_admins");
            for (int i = 0; i < jsonArray.length(); i++) {
                emails.add(jsonArray.get(i).toString());
            }
            directAdminSize = jsonArray.length();
            jsonArray = jsonObject.getJSONArray("direct_users");
            for (int i = 0; i < jsonArray.length(); i++) {
                emails.add(jsonArray.get(i).toString());
            }
        } catch (JSONException e) {
            ZLog.logException(e);
        }
    }

}
