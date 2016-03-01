package com.zulip.android;

import java.io.IOException;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.zulip.android.ZulipAsyncPushTask.AsyncTaskCompleteListener;

public class LoginActivity extends Activity implements View.OnClickListener,
            GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;

    LoginActivity that = this; // self-ref
    ZulipApp app;

    private ProgressDialog connectionProgressDialog;
    private int googleFailureCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (ZulipApp) getApplicationContext();

        setContentView(R.layout.login);

        ((Button) findViewById(R.id.login))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectionProgressDialog.show();
                        AsyncLogin alog = new AsyncLogin(that,
                                ((EditText) findViewById(R.id.username))
                                        .getText().toString(),
                                ((EditText) findViewById(R.id.password))
                                        .getText().toString());
                        // Remove the CPD when done
                        alog.setCallback(new AsyncTaskCompleteListener() {
                            @Override
                            public void onTaskComplete(String result) {
                                connectionProgressDialog.dismiss();
                            }

                            @Override
                            public void onTaskFailure(String result) {
                                connectionProgressDialog.dismiss();
                            }

                        });
                        alog.execute();
                    }
                });
        ((TextView) findViewById(R.id.legalTextView))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openLegal();
                    }
                });

        // Progress bar to be displayed if the connection failure is not
        // resolved.
        connectionProgressDialog = new ProgressDialog(this);
        connectionProgressDialog.setMessage("Signing in...");
        findViewById(R.id.sign_in_button).setOnClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_ACCOUNT_PICKER:
            if (data != null && data.getExtras() != null) {
                String accountName = data.getExtras().getString(
                        AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    this.app.setEmail(accountName);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            authWithGapps();
                        }
                    }).start();
                }
            }
        }
    }

    protected void openLegal() {
        Intent i = new Intent(this, LegalActivity.class);
        startActivityForResult(i, 0);
    }

    protected void openHome() {
        // Cancel before leaving activity to avoid leaking windows
        connectionProgressDialog.dismiss();
        Intent i = new Intent(this, ZulipActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (connectionProgressDialog.isShowing()) {
            // The user clicked the sign-in button already. Start to resolve
            // connection errors. Wait until onConnected() to dismiss the
            // connection dialog.
            if (result.hasResolution()) {
                try {
                    result.startResolutionForResult(this,
                            REQUEST_CODE_RESOLVE_ERR);
                } catch (SendIntentException e) {
                    // Yeah, no idea what to do here.
                }
            }
        }
    }

    public void quickRetry() {
        // We retry once every 500ms up to 3 times, then abort.
        googleFailureCount++;
        if (googleFailureCount < 3) {
            SystemClock.sleep(500);
            this.authWithGapps();
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionProgressDialog.dismiss();
                    Toast.makeText(that,
                            "Unable to connect with Google. Try again later.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void authWithGapps() {
        try {
            // Retrieve a token for the given account and scope. It
            // will always return either
            // a non-empty String or throw an exception.
            final String scope = "audience:server:client_id:835904834568-77mtr5mtmpgspj9b051del9i9r5t4g4n.apps.googleusercontent.com";
            final String token = GoogleAuthUtil.getToken(that,
                    this.app.getEmail(), scope);
            // Send token to the server to exchange for an API key
            final AsyncLogin loginTask = new AsyncLogin(that,
                    "google-oauth2-token", token);
            loginTask.setCallback(new AsyncTaskCompleteListener() {
                @Override
                public void onTaskComplete(String result) {
                    // No action needed
                }

                @Override
                public void onTaskFailure(String result) {
                    // Invalidate the token and try again, unless the user we
                    // are authenticating as is not registered or is disabled.
                    if (loginTask.userDefinitelyInvalid == false) {
                        GoogleAuthUtil.invalidateToken(that, token);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                quickRetry();
                            }
                        }).start();
                    } else {
                        connectionProgressDialog.dismiss();
                    }

                }
            });
            loginTask.execute();
        } catch (GooglePlayServicesAvailabilityException playEx) {
            final Dialog dia = GooglePlayServicesUtil.getErrorDialog(
                    playEx.getConnectionStatusCode(), this,
                    REQUEST_CODE_RESOLVE_ERR);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionProgressDialog.dismiss();
                    dia.show();
                }
            });
        } catch (UserRecoverableAuthException userAuthEx) {
            that.startActivityForResult(userAuthEx.getIntent(),
                    REQUEST_CODE_RESOLVE_ERR);
        } catch (IOException transientEx) {
            quickRetry();
        } catch (GoogleAuthException authEx) {
            ZLog.logException(authEx);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionProgressDialog.dismiss();
                    Toast.makeText(
                            that,
                            "Google Apps login not supported at this time, please contact support@zulip.com.",
                            Toast.LENGTH_LONG).show();
                    findViewById(R.id.sign_in_button).setEnabled(false);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button) {
            connectionProgressDialog.show();
            startActivityForResult(AccountPicker.newChooseAccountIntent(null,
                    null, new String[] { "com.google" }, false, null, null,
                    null, null), REQUEST_ACCOUNT_PICKER);
        }

    }
}
