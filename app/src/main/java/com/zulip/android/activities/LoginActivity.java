package com.zulip.android.activities;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.zulip.android.networking.AsyncFetchGoogleID;
import com.zulip.android.ZulipApp;
import com.zulip.android.networking.ZulipAsyncPushTask.AsyncTaskCompleteListener;
import com.zulip.android.networking.AsyncLogin;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final int REQUEST_CODE_SIGN_IN = 9001;

    private static final String AUTH_ENDPOINT_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT_URL = "https://www.googleapis.com/oauth2/v4/token";
    private static final String AUTH_CALLBACK = "com.zulip.android.dev:/oauth2callback";
    private static final String AUTH_INTENT_ACTION = "com.zulip.android.HANDLE_AUTHORIZATION_RESPONSE";

    private static final String USED_INTENT = "USED_INTENT";
    private ProgressDialog connectionProgressDialog;
    private EditText mServerEditText;
    private EditText mUserName;
    private EditText mPassword;
    private View mGoogleSignInButton;
    private CheckBox mUseZulipCheckbox;

    private static final String GOOGLE_SIGN = "GOOGLE_SIGN";

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
        checkIntent(getIntent());
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
                AsyncFetchGoogleID asyncFetchGoogleID = new AsyncFetchGoogleID(ZulipApp.get());
                asyncFetchGoogleID.setCallback(new AsyncTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(String result, JSONObject jsonObject) {
                        try {
                            JSONObject jsonObject1 = new JSONObject(result);
                            setupSignIn(jsonObject1.getString("google_client_id"));
                            connectionProgressDialog.dismiss();
                        } catch (JSONException e) {
                            ZLog.logException(e);
                        }
                    }

                    @Override
                    public void onTaskFailure(String result) {
                        connectionProgressDialog.dismiss();
                        if(result.contains("GOOGLE_CLIENT_ID is not configured")){
                         runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                                 googleClientNotConfigured();
                             }
                         });
                        }
                    }
                });
                asyncFetchGoogleID.execute();
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

    private void googleClientNotConfigured(){
        Toast.makeText(LoginActivity.this, R.string.google_client_error, Toast.LENGTH_SHORT).show();
        mGoogleSignInButton.setVisibility(View.GONE);
        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mGoogleSignInButton.setVisibility(View.VISIBLE);
                mServerEditText.removeTextChangedListener(this);
            }
        };
        mServerEditText.addTextChangedListener(textWatcher);
    }

    private void setupSignIn(String clientId) {
        AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                Uri.parse(AUTH_ENDPOINT_URL) /* auth endpoint */,
                Uri.parse(TOKEN_ENDPOINT_URL) /* token endpoint */
        );

        Uri redirectUri = Uri.parse(AUTH_CALLBACK);
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration,
                clientId,
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                redirectUri
        );
        builder.setScopes("profile", "email");
        AuthorizationRequest request = builder.build();
        AuthorizationService authorizationService = new AuthorizationService(LoginActivity.this);
        String action = AUTH_INTENT_ACTION;
        Intent postAuthorizationIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(LoginActivity.this, request.hashCode(), postAuthorizationIntent, 0);
        authorizationService.performAuthorizationRequest(request, pendingIntent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null) {
                    if (intent.getAction().equals(AUTH_INTENT_ACTION) && !intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
            }
        }
    }

    private void handleAuthorizationResponse(Intent intent) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        if (response != null) {
            Log.i(GOOGLE_SIGN, String.format("Handled Authorization Response %s ", authState.toJsonString()));
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w(GOOGLE_SIGN, "Token Exchange failed", exception);
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, exception);
                            AuthorizationService mAuthorizationService = new AuthorizationService(LoginActivity.this);
                            authState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                                @Override
                                public void execute(@Nullable final String accessToken, @Nullable final String idToken, @Nullable AuthorizationException exception) {
                                    final AsyncLogin loginTask = new AsyncLogin(LoginActivity.this, "google-oauth2-token", idToken, false);
                                    loginTask.setCallback(new AsyncTaskCompleteListener() {
                                        @Override
                                        public void onTaskComplete(String result, JSONObject object) {
                                            Log.d("RECIEVED", "onTaskComplete: " + result);
                                            try {
                                                ((ZulipApp) getApplication()).setEmail(object.getString("email"));
                                                connectionProgressDialog.dismiss();
                                            } catch (JSONException e) {
                                                ZLog.logException(e);
                                            }
                                        }

                                        @Override
                                        public void onTaskFailure(String result) {
                                            // Invalidate the token and try again, unless the user we
                                            // are authenticating as is not registered or is disabled.
                                            connectionProgressDialog.dismiss();
                                        }
                                    });
                                    loginTask.execute();
                                }
                            });
                            Log.i(GOOGLE_SIGN, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                        }
                    }
                }
            });
        }
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
        }
    }
}
