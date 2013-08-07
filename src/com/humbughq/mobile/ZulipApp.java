package com.humbughq.mobile;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class ZulipApp extends Application {
    private static final String USER_AGENT = "ZulipMobile";
    String client_id;
    Person you;
    SharedPreferences settings;
    String api_key;
    DatabaseHelper databaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        // This used to be from HumbugActivity.getPreferences, so we keep that
        // file name.
        this.settings = getSharedPreferences("HumbugActivity",
                Context.MODE_PRIVATE);

        this.you = new Person(null, settings.getString("email", null));
        this.api_key = settings.getString("api_key", null);

        if (api_key != null) {
            afterLogin();
        }
    }

    public void afterLogin() {
        databaseHelper = new DatabaseHelper(this);
    }

    public Boolean isLoggedIn() {
        return this.api_key != null;
    }

    /**
     * Determines the server URI applicable for the user.
     * 
     * @return either the production or staging server's URI
     */
    public String getServerURI() {
        if (you.getRealm().equals("zulip.com")
                || you.getRealm().equals("humbughq.com")) {
            return "https://staging.zulip.com/api/";
        }
        return "https://api.zulip.com/";
    }

    public String getApiKey() {
        return api_key;
    }

    public String getUserAgent() {
        try {
            return ZulipApp.USER_AGENT
                    + "/"
                    + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // This should… never happen, but okay.
            e.printStackTrace();
            return ZulipApp.USER_AGENT + "/unknown";
        }
    }

    public void setEmail(String email) {
        this.you = new Person(null, email);
    }

    public void setLoggedInApiKey(String apiKey) {
        this.api_key = apiKey;
        Editor ed = this.settings.edit();
        ed.putString("email", this.getEmail());
        ed.putString("api_key", api_key);
        ed.commit();
        afterLogin();
    }

    public void logOut() {
        Editor ed = this.settings.edit();
        ed.remove("email");
        ed.remove("api_key");
        ed.commit();
        this.api_key = null;
    }

    public String getEmail() {
        return this.you.getEmail();
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
