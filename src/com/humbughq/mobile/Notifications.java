package com.humbughq.mobile;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class Notifications {

    private static final String SENDER_ID = "835904834568";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    static final String TAG = "GCM";

    ZulipApp app;
    GoogleCloudMessaging gcm;
    String regid;

    Notifications(HumbugActivity activity) {
        this.app = activity.app;
        if (checkPlayServices(activity)) {
            gcm = GoogleCloudMessaging.getInstance(app);
            regid = getRegistrationId();

            if (regid.isEmpty()) {
                registerInBackground();
            } else {
                Log.i(TAG, "Already registered for GCM: " + regid);
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    private boolean checkPlayServices(HumbugActivity activity) {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(app);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
    }

    private String getRegistrationId() {
        String regID = app.settings.getString("gcm_reg_id", "");

        long registeredVersion = app.settings
                .getLong("gcm_reg_last_version", 0);
        long currentVersion = app.getAppVersion();

        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }

        return regID;
    }

    private void storeRegistrationId(Context context, String regId) {
        Editor ed = app.settings.edit();
        ed.putString("gcm_reg_id", regId);
        ed.putLong("gcm_reg_last_version", app.getAppVersion());
        ed.commit();
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(app);
                    }

                    int failures = 0;
                    while (true) {
                        try {
                            regid = gcm.register(SENDER_ID);
                            break;
                        } catch (IOException e) {
                            ZLog.logException(e);
                            failures += 1;
                            long backoff = (long) (Math.exp(failures / 2.0) * 1000);
                            Log.e("GCM", "Failure " + failures
                                    + ", sleeping for " + backoff);
                            SystemClock.sleep(backoff);
                        }
                    }

                    Log.i("GCM", "Device registered, registration ID=" + regid);

                    HTTPRequest request = new HTTPRequest(app);
                    request.setProperty("reg_id", regid);
                    request.execute("POST", "v1/users/me/android_gcm_reg_id");

                    // Persist the regID - no need to register again.
                    storeRegistrationId(app, regid);
                } catch (IOException ex) {
                    ZLog.logException(ex);
                }
                return null;
            }
        }.execute(null, null, null);
    }
}
