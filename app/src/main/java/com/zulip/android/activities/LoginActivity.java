package com.zulip.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.zulip.android.BuildConfig;
import com.zulip.android.R;
import com.zulip.android.networking.AsyncDevGetEmails;
import com.zulip.android.util.ZLog;
import com.zulip.android.ZulipApp;
import com.zulip.android.networking.ZulipAsyncPushTask.AsyncTaskCompleteListener;
import com.zulip.android.networking.AsyncLogin;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final int REQUEST_CODE_SIGN_IN = 9001;

    private ProgressDialog connectionProgressDialog;
    private EditText mServerEditText;
    private EditText mUserName;
    private EditText mPassword;
    private View mGoogleSignInButton;
    private CheckBox mUseZulipCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_login);
        setSupportActionBar(toolbar);

        // Progress bar to be displayed if the connection failure is not resolved.
        connectionProgressDialog = new ProgressDialog(this);
        connectionProgressDialog.setMessage(getString(R.string.signing_in));

        mUseZulipCheckbox = (CheckBox) findViewById(R.id.checkbox_usezulip);
        mServerEditText = (EditText) findViewById(R.id.server_url);
        mGoogleSignInButton = findViewById(R.id.google_sign_in_button);
        findViewById(R.id.google_sign_in_button).setOnClickListener(this);
        findViewById(R.id.zulip_login).setOnClickListener(this);
        mUseZulipCheckbox.setOnCheckedChangeListener(this);
        mUserName = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        if (!BuildConfig.DEBUG) findViewById(R.id.local_server_button).setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void saveServerURL() {
        if (mUseZulipCheckbox.isChecked()) {
            ((ZulipApp) getApplication()).useDefaultServerURL();
            return;
        }

        String serverURL = mServerEditText.getText().toString();
        int errorMessage = R.string.invalid_server_domain;

        if (serverURL.isEmpty()) {
            mServerEditText.setError(getString(errorMessage));
        }

        // add http if scheme is not included
        if (!serverURL.contains("://")) {
            serverURL = "http://" + serverURL;
        }

        Uri serverUri = Uri.parse(serverURL);
        if (serverUri.isRelative()) {
            serverUri = serverUri.buildUpon().scheme("http").build();
        }

        // if does not begin with "api.zulip.com" and if the path is empty, use "/api" as first segment in the path
        List<String> paths = serverUri.getPathSegments();
        if (!serverUri.getHost().startsWith("api.") && paths.isEmpty()) {
            serverUri = serverUri.buildUpon().appendEncodedPath("api/").build();
        }

        ((ZulipApp) getApplication()).setServerURL(serverUri.toString());
    }


    protected void openLegal() {
        Intent i = new Intent(this, LegalActivity.class);
        startActivityForResult(i, 0);
    }

    public void openHome() {
        // Cancel before leaving activity to avoid leaking windows
        connectionProgressDialog.dismiss();
        Intent i = new Intent(this, ZulipActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.google_sign_in_button:
                connectionProgressDialog.show();
                saveServerURL();
                Toast.makeText(this, getString(R.string.logging_into_server, ZulipApp.get().getServerURI()), Toast.LENGTH_SHORT).show();
                break;
            case R.id.zulip_login:
                if (!isInputValid()) {
                    return;
                }
                saveServerURL();
                Toast.makeText(this, getString(R.string.logging_into_server, ZulipApp.get().getServerURI()), Toast.LENGTH_SHORT).show();
                connectionProgressDialog.show();

                AsyncLogin alog = new AsyncLogin(LoginActivity.this,
                        mUserName.getText().toString(), mPassword.getText().toString(), false);
                // Remove the CPD when done
                alog.setCallback(new AsyncTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(String result, JSONObject object) {
                        connectionProgressDialog.dismiss();
                    }

                    @Override
                    public void onTaskFailure(String result) {
                        connectionProgressDialog.dismiss();
                    }

                });
                alog.execute();
                break;
            case R.id.legal_button:
                openLegal();
                break;
            case R.id.local_server_button:
                if (!isInputValidForDevAuth()) return;
                saveServerURL();
                connectionProgressDialog.show();
                AsyncDevGetEmails asyncDevGetEmails = new AsyncDevGetEmails(LoginActivity.this);
                asyncDevGetEmails.setCallback(new AsyncTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(String result, JSONObject jsonObject) {
                        connectionProgressDialog.dismiss();
                    }

                    @Override
                    public void onTaskFailure(String result) {
                        connectionProgressDialog.dismiss();
                    }
                });
                asyncDevGetEmails.execute();
            default:
                break;
        }
    }
    private boolean isInputValidForDevAuth() {
        boolean isValid = true;

        if (!mUseZulipCheckbox.isChecked()) {
            if (mServerEditText.length() == 0) {
                isValid = false;
                mServerEditText.setError(getString(R.string.server_domain_required));
            } else {
                String serverString = mServerEditText.getText().toString();
                if (!serverString.contains("://")) serverString = "https://" + serverString;

                if (!Patterns.WEB_URL.matcher(serverString).matches()) {
                    mServerEditText.setError(getString(R.string.invalid_domain));
                    isValid = false;
                }
            }
        }

        return isValid;
    }
    private boolean isInputValid() {
        boolean isValid = true;

        if (mPassword.length() == 0) {
            isValid = false;
            mPassword.setError(getString(R.string.password_required));
        }

        if (mUserName.length() == 0) {
            isValid = false;
            mUserName.setError(getString(R.string.username_required));
        }

        if (!mUseZulipCheckbox.isChecked()) {
            if (mServerEditText.length() == 0) {
                isValid = false;
                mServerEditText.setError(getString(R.string.server_domain_required));
            } else {
                String serverString = mServerEditText.getText().toString();
                if (!serverString.contains("://")) {
                    serverString = "https://" + serverString;
                }

                if (!Patterns.WEB_URL.matcher(serverString).matches()) {
                    mServerEditText.setError(getString(R.string.invalid_domain));
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mServerEditText.setVisibility(View.GONE);
            mServerEditText.getText().clear();
            mGoogleSignInButton.setVisibility(View.VISIBLE);
        } else {
            mServerEditText.setVisibility(View.VISIBLE);
            mGoogleSignInButton.setVisibility(View.GONE);
        }
    }
}
