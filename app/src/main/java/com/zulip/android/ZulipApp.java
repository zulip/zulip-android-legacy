package com.zulip.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.zulip.android.database.DatabaseHelper;
import com.zulip.android.models.Emoji;
import com.zulip.android.models.Message;
import com.zulip.android.models.Person;
import com.zulip.android.models.Presence;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.AsyncUnreadMessagesUpdate;
import com.zulip.android.networking.ZulipInterceptor;
import com.zulip.android.service.ZulipServices;
import com.zulip.android.util.ZLog;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Stores the global variables which are frequently used.
 *
 * {@link #max_message_id} This is the last Message ID stored in our database.
 * {@link #you} A reference to the user currently logged in.
 * {@link #api_key} Stores the API_KEY which was obtained from the server on successful authentication.
 * {@link #mutedTopics} Stores the concatenated ID for the stream and topic (Same strings as {@link Message#getIdForHolder()}
 * {@link #setupEmoji()} This is called to initialize and add records the existing emoticons in the assets folder.
 */
public class ZulipApp extends Application {
    private static final String API_KEY = "api_key";
    private static final String EMAIL = "email";
    private static ZulipApp instance;
    private static final String USER_AGENT = "ZulipAndroid";
    private static final String DEFAULT_SERVER_URL = "https://api.zulip.com/";
    private Person you;
    private SharedPreferences settings;
    private String api_key;
    private int max_message_id;
    private DatabaseHelper databaseHelper;
    private Set<String> mutedTopics;
    private static final String MUTED_TOPIC_KEY = "mutedTopics";
    private ZulipServices zulipServices;

    public Request goodRequest;
    public Request badRequest;

    /**
     * Handler to manage batching of unread messages
     */
    private Handler unreadMessageHandler;

    private String eventQueueId;
    private int lastEventId;

    private int pointer;

    // This object's intrinsic lock is used to prevent multiple threads from
    // making conflicting updates to ranges
    public Object updateRangeLock = new Object();

    /**
     * Mapping of email address to presence information for that user. This is
     * updated every 2 minutes by a background thread (see AsyncStatusUpdate)
     */
    public final Map<String, Presence> presences = new ConcurrentHashMap<>();

    /**
     * Queue of message ids to be marked as read. This queue should be emptied
     * every couple of seconds
     */
    public final Queue<Integer> unreadMessageQueue = new ConcurrentLinkedQueue<>();

    public static ZulipApp get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        ZulipApp.setInstance(this);

        // This used to be from HumbugActivity.getPreferences, so we keep that
        // file name.
        this.settings = getSharedPreferences("HumbugActivity",
                Context.MODE_PRIVATE);

        max_message_id = settings.getInt("max_message_id", -1);
        eventQueueId = settings.getString("eventQueueId", null);
        lastEventId = settings.getInt("lastEventId", -1);
        pointer = settings.getInt("pointer", -1);


        this.api_key = settings.getString(API_KEY, null);

        if (api_key != null) {
            afterLogin();
        }

        mutedTopics = new HashSet<>(settings.getStringSet(MUTED_TOPIC_KEY, new HashSet<String>()));
        // create unread message queue
        unreadMessageHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(android.os.Message message) {
                if (message.what == 0) {
                    AsyncUnreadMessagesUpdate task = new AsyncUnreadMessagesUpdate(
                            ZulipApp.this);
                    task.execute();
                }

                // documentation doesn't say what this value does for
                // Handler.Callback,
                // and Handler.handleMessage returns void
                // so this just returns true.
                return true;
            }
        });
    }

    public int getAppVersion() {
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package version: " + e);
        }
    }

    private void afterLogin() {
        String email = settings.getString(EMAIL, null);
        setEmail(email);
        setupEmoji();
    }

    public ZulipServices getZulipServices() {
        if(zulipServices == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            zulipServices = new Retrofit.Builder()
                    .client(new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS)
                            .addInterceptor(new ZulipInterceptor())
                            .addInterceptor(logging)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(getServerURI())
                    .build()
                    .create(ZulipServices.class);
        }
        return zulipServices;
    }

    /**
     * Fills the Emoji Table with the existing emoticons saved in the assets folder.
     */
    private void setupEmoji() {
        try {
            final RuntimeExceptionDao<Emoji, Object> dao = getDao(Emoji.class);
            if (dao.queryForAll().size() != 0) return;
            final String emojis[] = getAssets().list("emoji");
            TransactionManager.callInTransaction(getDatabaseHelper()
                            .getConnectionSource(),
                    new Callable<Void>() {
                        public Void call() throws Exception {
                            for (String newEmoji : emojis) {
                                dao.create(new Emoji(newEmoji));
                            }
                            return null;
                        }
                    });
        } catch (SQLException | IOException e) {
            ZLog.logException(e);
        }
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
        return settings.getString("server_url", DEFAULT_SERVER_URL);
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

    public void addToMutedTopics(JSONArray jsonArray) {
        Stream stream;

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONArray mutedTopic = jsonArray.getJSONArray(i);
                stream = Stream.getByName(this, mutedTopic.get(0).toString());
                mutedTopics.add(stream.getId() + mutedTopic.get(1).toString());
            } catch (JSONException e) {
                Log.e("JSONException", "JSON Is not correct", e);
            }
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(MUTED_TOPIC_KEY, new HashSet<>(mutedTopics));
        editor.apply();
    }

    public void setEmail(String email) {
        databaseHelper = new DatabaseHelper(this, email);
        this.you = Person.getOrUpdate(this, email, null, null);
    }

    public void setServerURL(String serverURL) {
        Editor ed = this.settings.edit();
        ed.putString("server_url", serverURL);
        ed.apply();
    }

    public void useDefaultServerURL() {
        setServerURL(DEFAULT_SERVER_URL);
    }

    public void setLoggedInApiKey(String apiKey) {
        this.api_key = apiKey;
        Editor ed = this.settings.edit();
        ed.putString(EMAIL, this.getEmail());
        ed.putString(API_KEY, api_key);
        ed.apply();
        afterLogin();
    }

    public void logOut() {
        Editor ed = this.settings.edit();
        ed.remove(EMAIL);
        ed.remove(API_KEY);
        ed.remove("server_url");
        ed.apply();
        this.api_key = null;
        setEventQueueId(null);
    }

    public String getEmail() {
        return you == null ? "" : you.getEmail();
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    @SuppressWarnings("unchecked")
    public <C, T> RuntimeExceptionDao<C, T> getDao(Class<C> cls) {
        try {
            return new RuntimeExceptionDao<>(
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

    public void setMaxMessageId(int maxMessageId) {
        this.max_message_id = maxMessageId;
        if (settings != null) {
            Editor ed = settings.edit();
            ed.putInt("max_message_id", maxMessageId);
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

    public void markMessageAsRead(Message message) {
        if (unreadMessageHandler == null) {
            Log.e("zulipApp",
                    "markMessageAsRead called before unreadMessageHandler was instantiated");
            return;
        }

        unreadMessageQueue.offer(message.getID());
        if (!unreadMessageHandler.hasMessages(0)) {
            unreadMessageHandler.sendEmptyMessageDelayed(0, 2000);
        }
    }

    public void muteTopic(Message message) {
        mutedTopics.add(message.concatStreamAndTopic());
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(MUTED_TOPIC_KEY, new HashSet<>(mutedTopics));
        editor.apply();
    }

    public boolean isTopicMute(Message message) {
        return mutedTopics.contains(message.concatStreamAndTopic());
    }

    public SharedPreferences getSettings() {
        return settings;
    }

    public Person getYou() {
        return you;
    }

    public static ZulipApp getInstance() {
        return instance;
    }

    private static void setInstance(ZulipApp instance) {
        ZulipApp.instance = instance;
    }

    public boolean isTopicMute(int id, String subject) {
        return mutedTopics.contains(id + subject);
    }

    public void syncPointer(final int mID) {

        getZulipServices().updatePointer(Integer.toString(mID))
        .enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                setPointer(mID);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                //do nothing.. don't want to mis-update the pointer.
            }
        });
    }
}
