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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.dao.ReferenceObjectCache;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.zulip.android.activities.ZulipActivity;
import com.zulip.android.database.DatabaseHelper;
import com.zulip.android.models.Emoji;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Person;
import com.zulip.android.models.Presence;
import com.zulip.android.networking.AsyncUnreadMessagesUpdate;
import com.zulip.android.networking.ZulipInterceptor;
import com.zulip.android.networking.response.UserConfigurationResponse;
import com.zulip.android.networking.response.events.EventsBranch;
import com.zulip.android.service.ZulipServices;
import com.zulip.android.util.GoogleAuthHelper;
import com.zulip.android.util.ZLog;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import okhttp3.OkHttpClient;
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
    private ZulipServices zulipServices;
    private ReferenceObjectCache objectCache;
    private ZulipActivity zulipActivity;

    public ZulipActivity getZulipActivity() {
        return zulipActivity;
    }

    public void setZulipActivity(ZulipActivity zulipActivity) {
        this.zulipActivity = zulipActivity;
    }

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
    public String tester;
    private Gson gson;

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
        this.settings = getSharedPreferences("HumbugActivity", Context.MODE_PRIVATE);

        max_message_id = settings.getInt("max_message_id", -1);
        eventQueueId = settings.getString("eventQueueId", null);
        lastEventId = settings.getInt("lastEventId", -1);
        pointer = settings.getInt("pointer", -1);


        this.api_key = settings.getString(API_KEY, null);

        if (api_key != null) {
            afterLogin();
        }

        // create unread message queue
        unreadMessageHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(android.os.Message message) {
                if (message.what == 0) {
                    AsyncUnreadMessagesUpdate task = new AsyncUnreadMessagesUpdate(ZulipApp.this);
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

    public ObjectCache getObjectCache() {
        if(objectCache == null) {
            objectCache = new ReferenceObjectCache(true);
        }
        return objectCache;
    }

    private void afterLogin() {
        String email = settings.getString(EMAIL, null);
        setEmail(email);
        setupEmoji();
    }

    public ZulipServices getZulipServices() {
        if (zulipServices == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
            zulipServices = new Retrofit.Builder()
                    .client(new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS)
                            .addInterceptor(new ZulipInterceptor())
                            .addInterceptor(logging)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create(getGson()))
                    .baseUrl(getServerURI())
                    .build()
                    .create(ZulipServices.class);
        }
        return zulipServices;
    }

    public void setZulipServices(ZulipServices zulipServices) {
        this.zulipServices = zulipServices;
    }

    public Gson getGson() {
        if(gson == null) {
            gson = buildGson();
        }
        return gson;
    }

    private Gson buildGson() {
        final Gson naiveGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return new Date(json.getAsJsonPrimitive().getAsLong() * 1000);
                    }
                })
                .registerTypeAdapter(MessageType.class, new JsonDeserializer<MessageType>() {
                    @Override
                    public MessageType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return json.getAsString().equalsIgnoreCase("stream") ? MessageType.STREAM_MESSAGE : MessageType.PRIVATE_MESSAGE;
                    }
                })
                .create();
        final Gson nestedGson = new GsonBuilder()
                .registerTypeAdapter(Message.class, new JsonDeserializer<Message>() {
                    @Override
                    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

                        if(BuildConfig.DEBUG) {
                            Log.d("RAW MESSAGES", json.toString());
                        }
                        Message genMess;
                        if("stream".equalsIgnoreCase(json.getAsJsonObject().get("type").getAsString())) {
                            Message.ZulipStreamMessage msg = naiveGson.fromJson(json, Message.ZulipStreamMessage.class);
                            msg.setRecipients(msg.getDisplayRecipient());
                            genMess = msg;
                        } else {
                            Message.ZulipDirectMessage msg = naiveGson.fromJson(json, Message.ZulipDirectMessage.class);
                            if(msg.getDisplayRecipient() != null) {
                                msg.setRecipients(msg.getDisplayRecipient().toArray(new Person[msg.getDisplayRecipient().size()]));
                            }

                            msg.setContent(Message.formatContent(msg.getFormattedContent(), ZulipApp.get()).toString());
                            genMess = msg;
                        }
                        if(genMess._history != null && genMess._history.size() != 0) {
                            genMess.updateFromHistory(genMess._history.get(0));
                        }
                        return genMess;
                    }
                }).create();

        return new GsonBuilder()
                .registerTypeAdapter(UserConfigurationResponse.class, new TypeAdapter<UserConfigurationResponse>() {

                    @Override
                    public void write(JsonWriter out, UserConfigurationResponse value) throws IOException {
                        nestedGson.toJson(nestedGson.toJsonTree(value), out);
                    }

                    @Override
                    public UserConfigurationResponse read(JsonReader in) throws IOException {
                        UserConfigurationResponse res = nestedGson.fromJson(in, UserConfigurationResponse.class);

                        RuntimeExceptionDao<Person, Object> personDao = ZulipApp.this.getDao(Person.class);
                        for (int i = 0; i < res.getRealmUsers().size(); i++) {

                            Person currentPerson = res.getRealmUsers().get(i);
                            Person foundPerson = null;
                            try {
                                foundPerson = personDao.queryBuilder().where().eq(Person.EMAIL_FIELD, currentPerson.getEmail()).queryForFirst();
                                if(foundPerson != null) {
                                    currentPerson.setId(foundPerson.getId());
                                }
                            } catch (SQLException e) {
                                ZLog.logException(e);
                            }
                        }

                        return res;
                    }
                })
                .registerTypeAdapter(EventsBranch.class, new JsonDeserializer<EventsBranch>() {
                    @Override
                    public EventsBranch deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        EventsBranch invalid = nestedGson.fromJson(json, EventsBranch.class);
                        if(BuildConfig.DEBUG) {
                            Log.d("RAW EVENTS", json.toString());
                        }
                        Class<? extends EventsBranch> t = EventsBranch.BranchType.fromRawType(invalid);
                        if(t != null) {
                            return nestedGson.fromJson(json, t);
                        }
                        Log.w("GSON", "Attempted to deserialize and unregistered EventBranch... See EventBranch.BranchType");
                        return invalid;
                    }
                })
                .registerTypeAdapter(Message.class, nestedGson.getAdapter(Message.class))
                .create();
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

    public String getServerHostUri() {
        String uri = settings.getString("server_url", DEFAULT_SERVER_URL);
        if (uri.contains("/api/")) {
            uri = uri.replace("/api/", "/");
        } else if (uri.contains("/api.")) {
            uri = uri.replace("/api.", "/");
        }
        return uri;
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

    public void setServerURL(String serverURL) {
        Editor ed = this.settings.edit();
        ed.putString("server_url", serverURL);
        ed.apply();
    }

    public void useDefaultServerURL() {
        setServerURL(DEFAULT_SERVER_URL);
    }

    public void setLoggedInApiKey(String apiKey, String email) {
        this.api_key = apiKey;
        Editor ed = this.settings.edit();
        ed.putString(EMAIL, email);
        ed.putString(API_KEY, apiKey);
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

        new GoogleAuthHelper().logOutGoogleAuth();
    }

    public String getEmail() {
        return you == null ? "" : you.getEmail();
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    @SuppressWarnings("unchecked")
    public <C, T> RuntimeExceptionDao<C, T> getDao(Class<C> cls, boolean useCache) {
        try {
            RuntimeExceptionDao<C, T> ret = new RuntimeExceptionDao<>(
                    (Dao<C, T>) databaseHelper.getDao(cls));
            if(useCache) {
                ret.setObjectCache(getObjectCache());
            }
            return ret;
        } catch (SQLException e) {
            // Well that's sort of awkward.
            throw new RuntimeException(e);
        }
    }

    public <C, T> RuntimeExceptionDao<C, T> getDao(Class<C> cls) {
        return getDao(cls, false);
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

    public SharedPreferences getSettings() {
        return settings;
    }

    public Person getYou() {
        return you;
    }

    private static void setInstance(ZulipApp instance) {
        ZulipApp.instance = instance;
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
