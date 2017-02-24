package com.zulip.android.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.zulip.android.BuildConfig;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.networking.AsyncDevGetEmails;
import com.zulip.android.networking.ZulipAsyncPushTask;
import com.zulip.android.networking.response.LoginResponse;
import com.zulip.android.networking.response.ZulipBackendResponse;
import com.zulip.android.networking.util.DefaultCallback;
import com.zulip.android.util.ActivityTransitionAnim;
import com.zulip.android.util.AnimationHelper;
import com.zulip.android.util.CommonProgressDialog;
import com.zulip.android.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Activity to Login through various backends on a specified server.
 * Currently supported LoginAuths are Emailbackend and DevAuthBackend.
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {
    //region state-restoration
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String SERVER_IN = "serverIn";
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final int REQUEST_CODE_SIGN_IN = 9001;
    private CommonProgressDialog commonProgressDialog;
    private GoogleApiClient mGoogleApiClient;
    private EditText mServerEditText;
    private EditText mUserName;
    private EditText mPassword;
    private ImageView mShowPassword;
    private EditText serverIn;
    private boolean skipAnimations = false;
    //endregion

    private View mGoogleSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_login);
        setSupportActionBar(toolbar);

        // Progress bar to be displayed if the connection failure is not resolved.
        commonProgressDialog = new CommonProgressDialog(this);
        mServerEditText = (EditText) findViewById(R.id.server_url);
        mGoogleSignInButton = findViewById(R.id.google_sign_in_button);
        findViewById(R.id.google_sign_in_button).setOnClickListener(this);
        findViewById(R.id.zulip_login).setOnClickListener(this);
        mUserName = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mShowPassword = (ImageView) findViewById(R.id.showPassword);
        serverIn = (EditText) findViewById(R.id.server_url_in);
        String serverUrl = getIntent().getStringExtra(Constants.SERVER_URL);
        if (serverUrl != null) {
            serverIn.setText(serverUrl);
        }
        findViewById(R.id.server_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForError();
            }
        });

        findViewById(R.id.input_another_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AnimationHelper.hideView(findViewById(R.id.serverInput), 100);
                AnimationHelper.showView(findViewById(R.id.serverFieldLayout), 201);
                mServerEditText.setText("");
                mServerEditText.setEnabled(false);
                findViewById(R.id.passwordAuthLayout).setVisibility(View.GONE);
                findViewById(R.id.google_sign_in_button).setVisibility(View.GONE);
                findViewById(R.id.local_server_button).setVisibility(View.GONE);
                //remove error from all editText as user now corrected serverUrl
                mPassword.setError(null);
                mUserName.setError(null);
                serverIn.setError(null);
                mServerEditText.setError(null);
            }
        });
        //restore instance state on orientation change
        if (savedInstanceState != null) {
            skipAnimations = true;
            serverIn.setText(savedInstanceState.getString(SERVER_IN));
            ((Button) findViewById(R.id.server_btn)).performClick();
            mUserName.setText(savedInstanceState.getString(USERNAME));
            mPassword.setText(savedInstanceState.getString(PASSWORD));
        }

        mShowPassword.setVisibility(View.GONE);
        mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (mPassword.getText().length() > 0) {
                    mShowPassword.setVisibility(View.VISIBLE);
                } else {
                    mShowPassword.setVisibility(View.GONE);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        mShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mShowPassword.getTag().toString().equals("visible")) {
                    mShowPassword.setTag("hide");
                    mShowPassword.setImageResource(R.drawable.ic_visibility_off_black_24dp);
                    mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    mPassword.setSelection(mPassword.length());
                } else {
                    mShowPassword.setTag("visible");
                    mShowPassword.setImageResource(R.drawable.ic_visibility_black_24dp);
                    mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mPassword.setSelection(mPassword.length());
                }
            }
        });
    }

    private void showLoginFields() {
        AnimationHelper.showView(findViewById(R.id.serverInput), skipAnimations ? 0 : 201);
        AnimationHelper.hideView(findViewById(R.id.serverFieldLayout), skipAnimations ? 0 : 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
                handleSignInResult(result);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commonProgressDialog != null && commonProgressDialog.isShowing()) {
            commonProgressDialog.dismiss();
        }
    }

    private void checkForError() {
        String serverURL = serverIn.getText().toString();

        // trim leading or trailing white spaces in Url
        serverURL = serverURL.trim();

        int errorMessage = R.string.invalid_server_domain;
        String httpScheme = (BuildConfig.DEBUG) ? "http" : "https";

        if (serverURL.isEmpty()) {
            serverIn.setError(getString(errorMessage));
            return;
        }

        // add http or https if scheme is not included
        if (!serverURL.contains("://")) {
            serverURL = httpScheme + "://" + serverURL;
            if (BuildConfig.DEBUG)
                showHTTPDialog(serverURL); //Ask for http or https if in non-prod builds otherwise if in prod then use https
            else showBackends(httpScheme, serverURL);
        } else {
            Uri serverUri = Uri.parse(serverURL);

            if (!BuildConfig.DEBUG && serverUri.getScheme().equals("http")) { //Production build and not https
                showHTTPDialog(serverURL);
            } else {
                showBackends(serverUri.getScheme(), serverURL);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Boolean inLogin = mUserName.isShown();
        savedInstanceState.putString(SERVER_IN, mServerEditText.getText().toString());
        savedInstanceState.putString(USERNAME, mUserName.getText().toString());
        savedInstanceState.putString(PASSWORD, mPassword.getText().toString());
    }

    private boolean isUrlValid(String url) {
        if (BuildConfig.DEBUG) {
            return Patterns.WEB_URL.matcher(String.valueOf(url)).matches() ||
                    Patterns.IP_ADDRESS.matcher(url).matches();
        } else {
            return Patterns.WEB_URL.matcher(String.valueOf(url)).matches();
        }
    }

    private void showBackends(String httpScheme, String serverURL) {
        commonProgressDialog.showWithMessage(getString(R.string.connecting_to_server));
        // if server url does not end with "/", then append it
        if (!serverURL.endsWith("/")) {
            serverURL = serverURL + "/";
        }

        if (!isUrlValid(serverURL)) {
            Toast.makeText(LoginActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
            commonProgressDialog.dismiss();
            return;
        }

        Uri serverUri = Uri.parse(serverURL);

        serverUri = serverUri.buildUpon().scheme(httpScheme).build();

        // display server url with http scheme used
        serverIn.setText(serverUri.toString().toLowerCase());
        mServerEditText.setText(serverUri.toString().toLowerCase());
        mServerEditText.setEnabled(false);

        // if server url does not end with "api/" or if the path is empty, use "/api" as last segment in the path
        List<String> paths = serverUri.getPathSegments();
        if (paths.isEmpty() || !paths.get(paths.size() - 1).equals("api")) {
            serverUri = serverUri.buildUpon().appendEncodedPath("api/").build();
        }

        ((ZulipApp) getApplication()).setServerURL(serverUri.toString().toLowerCase());

        // create new zulipServices object every time by setting it to null
        getApp().setZulipServices(null);

        getServices()
                .getAuthBackends()
                .enqueue(new DefaultCallback<ZulipBackendResponse>() {

                    @Override
                    public void onSuccess(Call<ZulipBackendResponse> call, Response<ZulipBackendResponse> response) {
                        View view = LoginActivity.this.getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }

                        if (response.body().isPassword()) {
                            findViewById(R.id.passwordAuthLayout).setVisibility(View.VISIBLE);
                        }

                        if (response.body().isGoogle()) {
                            findViewById(R.id.google_sign_in_button).setVisibility(View.VISIBLE);
                        }

                        if (response.body().isDev()) {
                            findViewById(R.id.local_server_button).setVisibility(View.VISIBLE);
                        }
                        commonProgressDialog.dismiss();
                        showLoginFields();
                    }

                    @Override
                    public void onError(Call<ZulipBackendResponse> call, Response<ZulipBackendResponse> response) {
                        Toast.makeText(LoginActivity.this, R.string.toast_login_failed_fetching_backends, Toast.LENGTH_SHORT).show();
                        commonProgressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(Call<ZulipBackendResponse> call, Throwable t) {
                        super.onFailure(call, t);
                        if (!isNetworkAvailable()) {
                            Toast.makeText(LoginActivity.this, R.string.toast_no_internet_connection, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
                        }
                        commonProgressDialog.dismiss();
                    }
                });

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void showHTTPDialog(final String serverURL) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.http_or_https)
                .setMessage(((BuildConfig.DEBUG) ? R.string.http_message_debug : R.string.http_message))
                .setPositiveButton(R.string.use_https, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showBackends("https", serverURL);
                    }
                })
                .setNeutralButton(R.string.use_http, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showBackends("http", serverURL);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void handleSignInResult(final GoogleSignInResult result) {
        Log.d("Login", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();

            // if there's a problem with fetching the account, bail
            if (account == null) {
                commonProgressDialog.dismiss();
                Toast.makeText(LoginActivity.this, R.string.google_app_login_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            getServices()
                    .login("google-oauth2-token", account.getIdToken())
                    .enqueue(new DefaultCallback<LoginResponse>() {

                        @Override
                        public void onSuccess(Call<LoginResponse> call, Response<LoginResponse> response) {
                            commonProgressDialog.dismiss();
                            getApp().setLoggedInApiKey(response.body().getApiKey(), response.body().getEmail());
                            openHome();
                        }

                        @Override
                        public void onError(Call<LoginResponse> call, Response<LoginResponse> response) {
                            commonProgressDialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            super.onFailure(call, t);
                            commonProgressDialog.dismiss();
                        }
                    });

        } else {
            // something bad happened. whoops.
            commonProgressDialog.dismiss();
            Toast.makeText(LoginActivity.this, R.string.google_app_login_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void openLegal() {
        Intent i = new Intent(this, LegalActivity.class);
        startActivityForResult(i, 0);
    }

    public void openHome() {
        // Cancel before leaving activity to avoid leaking windows
        commonProgressDialog.dismiss();
        Intent i = new Intent(this, ZulipActivity.class);
        startActivity(i);

        // activity transition animation
        ActivityTransitionAnim.transition(this);

        finish();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (commonProgressDialog.isShowing()) {
            // The user clicked the sign-in button already. Start to resolve
            // connection errors. Wait until onConnected() to dismiss the
            // connection dialog.
            if (result.hasResolution()) {
                try {
                    result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                } catch (SendIntentException e) {
                    Log.e(TAG, e.getMessage(), e);
                    // Yeah, no idea what to do here.
                    commonProgressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, R.string.google_app_login_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                commonProgressDialog.dismiss();
                if (!isNetworkAvailable()) {
                    Toast.makeText(LoginActivity.this, R.string.toast_no_internet_connection, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.google_app_login_failed, Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    private void setupGoogleSignIn() {
        if (mGoogleApiClient == null) {
            GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(LoginActivity.this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                    .addOnConnectionFailedListener(LoginActivity.this)
                    .build();

            mGoogleApiClient.connect();

            allowUserToPickAccount();
        } else {
            allowUserToPickAccount();
        }

    }

    private void allowUserToPickAccount() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.google_sign_in_button:
                commonProgressDialog.showWithMessage(getString(R.string.signing_in));
                setupGoogleSignIn();
                break;
            case R.id.zulip_login:
                if (!isInputValid()) {
                    return;
                }
                commonProgressDialog.showWithMessage(getString(R.string.signing_in));
                String username = mUserName.getText().toString();
                String password = mPassword.getText().toString();
                getServices()
                        .login(username, password)
                        .enqueue(new DefaultCallback<LoginResponse>() {

                            @Override
                            public void onSuccess(Call<LoginResponse> call, Response<LoginResponse> response) {
                                commonProgressDialog.dismiss();
                                getApp().setLoggedInApiKey(response.body().getApiKey(), response.body().getEmail());
                                openHome();
                            }


                            @Override
                            public void onError(Call<LoginResponse> call, Response<LoginResponse> response) {
                                commonProgressDialog.dismiss();
                                if (response != null && response.errorBody() != null) {
                                    try {
                                        JSONObject message = new JSONObject(response.errorBody().string());
                                        Toast.makeText(LoginActivity.this, message.getString("msg"), Toast.LENGTH_LONG).show();
                                    } catch (JSONException | IOException e) {
                                        // oops
                                        Toast.makeText(LoginActivity.this, R.string.login_activity_toast_login_error, Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    if (!isNetworkAvailable()) {
                                        Toast.makeText(LoginActivity.this, R.string.toast_no_internet_connection, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, R.string.login_activity_toast_login_error, Toast.LENGTH_LONG).show();
                                    }

                                }
                            }

                            @Override
                            public void onFailure(Call<LoginResponse> call, Throwable t) {
                                super.onFailure(call, t);
                                commonProgressDialog.dismiss();
                                if (!isNetworkAvailable()) {
                                    Toast.makeText(LoginActivity.this, R.string.toast_no_internet_connection, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, R.string.login_activity_toast_login_error, Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                break;
            case R.id.legal_button:
                openLegal();
                break;
            case R.id.local_server_button:
                if (!isInputValidForDevAuth()) return;
                commonProgressDialog.showWithMessage(getString(R.string.signing_in));
                AsyncDevGetEmails asyncDevGetEmails = new AsyncDevGetEmails(LoginActivity.this);
                asyncDevGetEmails.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(String result, JSONObject jsonObject) {
                        commonProgressDialog.dismiss();
                    }

                    @Override
                    public void onTaskFailure(String result) {
                        commonProgressDialog.dismiss();
                    }
                });
                asyncDevGetEmails.execute();
                break;
            case R.id.register:
                openRegister();
                break;
            default:
                break;
        }
    }

    private void openRegister() {
        Uri uri;
        if (serverIn == null || serverIn.getText().toString().isEmpty() || serverIn.getText().toString().equals("")) {
            return;
        } else {
            uri = Uri.parse(serverIn.getText().toString() + "register");
        }
        if (Build.VERSION.SDK_INT < 15) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return;
        }
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent intent = builder.build();
        intent.launchUrl(LoginActivity.this, uri);
    }

    private boolean isInputValidForDevAuth() {
        boolean isValid = true;

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
        return isValid;
    }
}
