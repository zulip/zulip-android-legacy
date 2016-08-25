package com.zulip.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.zulip.android.R;
import com.zulip.android.networking.AsyncDevGetEmails;
import com.zulip.android.networking.response.LoginResponse;
import com.zulip.android.networking.util.DefaultCallback;
import com.zulip.android.util.AuthClickListener;
import com.zulip.android.util.ZLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Activity where the Emails for the DevAuthBackend are displayed.
 */
public class DevAuthActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private ProgressDialog connectionProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_auth);
        String json = getIntent().getStringExtra(AsyncDevGetEmails.EMAIL_JSON);
        recyclerView = (RecyclerView) findViewById(R.id.devAuthRecyclerView);
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
        AuthEmailAdapter authEmailAdapter = new AuthEmailAdapter(emails, directAdminSize, DevAuthActivity.this);
        recyclerView.setAdapter(authEmailAdapter);
        authEmailAdapter.setOnItemClickListener(new AuthClickListener() {
            @Override
            public void onItemClick(String email) {
                if(email.contains("@")) {
                    getApp().setEmail(email);
                }
                getServices()
                        .loginDEV(email)
                        .enqueue(new DefaultCallback<LoginResponse>() {
                            @Override
                            public void onSuccess(Call<LoginResponse> call, Response<LoginResponse> response) {
                                super.onSuccess(call, response);
                                getApp().setLoggedInApiKey(response.body().getApiKey());
                                openHome();
                            }
                        });
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(DevAuthActivity.this) {
        });
    }

    public void openHome() {
        // Cancel before leaving activity to avoid leaking windows
        connectionProgressDialog.dismiss();
        Intent i = new Intent(this, ZulipActivity.class);
        startActivity(i);
        finish();
    }
}
