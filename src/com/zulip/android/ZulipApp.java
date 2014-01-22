package com.zulip.android;

import java.sql.SQLException;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;

public class ZulipApp extends Application {
    private static ZulipApp instance;
    private static final String USER_AGENT = "ZulipMobile";
    Person you;
    SharedPreferences settings;
    String api_key;
    private int max_message_id;
    DatabaseHelper databaseHelper;

    private String eventQueueId;
    private int lastEventId;

    private int pointer;

    // This object's intrinsic lock is used to prevent multiple threads from
    // making conflicting updates to ranges
    public Object updateRangeLock = new Object();

    public static ZulipApp get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ZulipApp.instance = this;

        // This used to be from HumbugActivity.getPreferences, so we keep that
        // file name.
        this.settings = getSharedPreferences("HumbugActivity",
                Context.MODE_PRIVATE);

        max_message_id = settings.getInt("max_message_id", -1);
        eventQueueId = settings.getString("eventQueueId", null);
        lastEventId = settings.getInt("lastEventId", -1);
        pointer = settings.getInt("pointer", -1);

        this.api_key = settings.getString("api_key", null);

        if (api_key != null) {
            afterLogin();
        }
    }

    int getAppVersion() {
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package version: " + e);
        }
    }

    public void afterLogin() {
        String email = settings.getString("email", null);
        setEmail(email);
    }

    public Boolean isLoggedIn() {
        return this.api_key != null;
    }

    public void clearConnectionState() {
        setEventQueueId(null);
    }

    /**
     * Determines the server URI applicable for the user.
     * 
     * @return either the production or staging server's URI
     */
    public String getServerURI() {
        if (getEmail().equals("iago@zulip.com")) {
            return "http://10.0.2.2:9991/api/";
        }
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
            ZLog.logException(e);
            return ZulipApp.USER_AGENT + "/unknown";
        }
    }

    public void setEmail(String email) {
        databaseHelper = new DatabaseHelper(this, email);
        this.you = Person.getOrUpdate(this, email, null, null);
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
        setEventQueueId(null);
    }

    public String getEmail() {
        return this.you.getEmail();
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    @SuppressWarnings("unchecked")
    public <C, T> RuntimeExceptionDao<C, T> getDao(Class<C> cls) {
        try {
            return new RuntimeExceptionDao<C, T>(
                    (Dao<C, T>) databaseHelper.getDao(cls));
        } catch (SQLException e) {
            // Well that's sort of awkward.
            throw new RuntimeException(e);
        }
    }

    public void setContext(Context targetContext) {
        this.attachBaseContext(targetContext);
    }

    public String getEventQueueId() {
        return eventQueueId;
    }

    public void setEventQueueId(String eventQueueId) {
        this.eventQueueId = eventQueueId;
        Editor ed = settings.edit();
        ed.putString("eventQueueId", this.eventQueueId);
        ed.apply();
    }

    public int getLastEventId() {
        return lastEventId;
    }

    public void setLastEventId(int lastEventId) {
        this.lastEventId = lastEventId;
        Editor ed = settings.edit();
        ed.putInt("lastEventId", lastEventId);
        ed.apply();
    }

    public int getMaxMessageId() {
        return max_message_id;
    }

    public void setMaxMessageId(int max_message_id) {
        this.max_message_id = max_message_id;
        if (settings != null) {
            Editor ed = settings.edit();
            ed.putInt("max_message_id", max_message_id);
            ed.apply();
        }
    }

    public int getPointer() {
        return pointer;
    }

    public void setPointer(int pointer) {
        this.pointer = pointer;
        Editor ed = settings.edit();
        ed.putInt("pointer", pointer);
        ed.apply();
    }

    public void resetDatabase() {
        databaseHelper.resetDatabase(databaseHelper.getWritableDatabase());
    }

    public void onResetDatabase() {
        setPointer(-1);
        setMaxMessageId(-1);
        setLastEventId(-1);
        setEventQueueId(null);
    }
}
