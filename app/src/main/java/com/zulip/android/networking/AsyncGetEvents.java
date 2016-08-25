package com.zulip.android.networking;

import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.zulip.android.ZulipApp;
import com.zulip.android.activities.ZulipActivity;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageRange;
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.response.UserConfigurationResponse;
import com.zulip.android.service.ZulipServices;
import com.zulip.android.util.ZLog;
import com.zulip.android.widget.ZulipWidget;

import org.apache.commons.lang.time.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

/**
 * A background task which asynchronously fetches the updates from the server.
 * The run method {@link #run()} which has an infinite loop and keeps fetching the latest updates
 * and handles the events appropriately.
 * If the user is not registered to a queue this registers {@link #register()} for a new lastEventId and queueID
 *
 * lastEventId {@link ZulipApp#lastEventId} which is used to fetch after this ID events from the server.
 */
public class AsyncGetEvents extends Thread {
    private static final String TAG = "AsyncGetEvents";
    private static final String ASYNC_GET_EVENTS = "asyncGetEvents";
    private static final String POINTER = "pointer";
    private ZulipActivity activity;
    private ZulipApp app;
    private static int interval = 1000;
    private boolean calledFromWidget = false;

    private boolean keepThisRunning = true;
    private HTTPRequest request;

    private AsyncGetEvents that = this;
    private int failures = 0;
    private boolean registeredOrGotEventsThisRun;

    public AsyncGetEvents(ZulipActivity zulipActivity) {
        super();
        app = (ZulipApp) zulipActivity.getApplication();
        activity = zulipActivity;
        request = new HTTPRequest(app);
    }

    public AsyncGetEvents(ZulipApp zulipApp, int interval) {
        super();
        app = zulipApp;
        activity = null;
        request = new HTTPRequest(app);
        calledFromWidget = true;
        this.interval = interval;
    }

    public void start() {
        registeredOrGotEventsThisRun = false;
        if (!calledFromWidget) {
            super.start();
        }
    }

    public void abort() {
        // TODO: does this have race conditions? (if the thread is not in a
        // request when called)
        Log.i(ASYNC_GET_EVENTS, "Interrupting thread");
        keepThisRunning = false;
        request.abort();
    }

    private void backoff(Exception e) {
        if (e != null) {
            ZLog.logException(e);
        }
        failures += 1;
        long backoff = (long) (Math.exp(failures / 2.0) * 1000);
        Log.e(ASYNC_GET_EVENTS, "Failure " + failures + ", sleeping for "
                + backoff);
        SystemClock.sleep(backoff);
    }

    /**
     * Registers for a new event queue with the Zulip API
     */
    private void register() throws JSONException, IOException {
        retrofit2.Response<UserConfigurationResponse> response = app.getZulipServices()
                .register(true)
                .execute();
        if(response.isSuccessful()) {
            UserConfigurationResponse res = response.body();
            app.tester = app.getEventQueueId();
            app.setEventQueueId(res.getQueueId());
            app.setLastEventId(res.getLastEventId());
            app.setPointer(res.getPointer());
            app.setMaxMessageId(res.getMaxMessageId());
            registeredOrGotEventsThisRun = true;
            processRegister(res);
        }
    }

    private void processRegister(final UserConfigurationResponse response) {
        // In task thread
        try {

            TransactionManager.callInTransaction(app.getDatabaseHelper()
                    .getConnectionSource(), new Callable<Void>() {
                public Void call() throws Exception {

                    // Get subscriptions
                    List<Stream> subscriptions = response.getSubscriptions();

                    //todo
                    app.addToMutedTopics(response.getMutedTopics());

                    RuntimeExceptionDao<Stream, Object> streamDao = app
                            .getDao(Stream.class);
                    Log.i("stream", "" + subscriptions.size() + " streams");

                    // Mark all existing subscriptions as not subscribed
                    streamDao.updateBuilder().updateColumnValue(
                            Stream.SUBSCRIBED_FIELD, false);

                    for (int i = 0; i < subscriptions.size(); i++) {
                        Stream stream = subscriptions.get(i);
                        stream.getParsedColor();
                        stream.setSubscribed(true);
                        streamDao.createOrUpdate(stream);
                    }

                    // Get people
                    List<Person> people = response.getRealmUsers();

                    RuntimeExceptionDao<Person, Object> personDao = app
                            .getDao(Person.class);

                    // Mark all existing people as inactive
                    personDao.updateBuilder().updateColumnValue(
                            Person.ISACTIVE_FIELD, false);


                    for (int i = 0; i < people.size(); i++) {

                        Person person = people.get(i);
                        person.setActive(true);
                        personDao.createOrUpdate(person);
                    }
                    return null;
                }
            });

            if (!calledFromWidget) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        that.activity.getPeopleAdapter().refresh();
                        activity.onReadyToDisplay(true);
                        activity.checkAndSetupStreamsDrawer();
                    }
                });
            }
        }
        catch (SQLException e) {
            ZLog.logException(e);
        }
    }


    public void run() {
        try {
            while (keepThisRunning) {
                try {
                    request.clearProperties();
                    if (app.getEventQueueId() == null) {
                        register();
                    }

                    new Retrofit.Builder()
                            .client(new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS)
                                    .addInterceptor(new ZulipInterceptor())
                                    .build())
                            .addConverterFactory(new ZulipServices.ToStringConverterFactory())
                            .baseUrl(app.getServerURI())
                            .build()
                            .create(ZulipServices.class)
                            .getEvents(null, app.getLastEventId(), app.getEventQueueId())
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                                    String k = "";
                                    String kz = "";
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    String k  = "";
                                    String kz  = "";
                                }
                            });


                    request.setProperty("queue_id", app.getEventQueueId());
                    request.setProperty("last_event_id", "" + app.getLastEventId());
                    if (!registeredOrGotEventsThisRun) {
                        request.setProperty("dont_block", "true");
                    }
                    request.setMethodAndUrl("GET", "v1/events");
                    Response httpResponse = request.execute();

                    String json = httpResponse.body().string();
                    if (!httpResponse.isSuccessful()) {
                        String msg = httpResponse.message();
                        if (httpResponse.code() == 400 && (json.contains("Bad event queue id")
                                || msg.contains("too old"))) {
                            // Queue dead. Register again.
                            Log.w(ASYNC_GET_EVENTS, "Queue dead");
                            app.setEventQueueId(null);
                            Log.i("WRONG", "run: " + json);
                            continue;
                        }
                        Log.i("WRONG", "run: " + json);
                        backoff(null);
                    } else {
                        Log.i("OkHttp200GE", json);
                        JSONObject response = new JSONObject(json);

                        JSONArray events = response.getJSONArray("events");
                        if (events.length() > 0) {
                            this.processEvents(events);

                            JSONObject lastEvent = events.getJSONObject(events
                                    .length() - 1);
                            app.setLastEventId(lastEvent.getInt("id"));

                            failures = 0;
                        }

                        if (!registeredOrGotEventsThisRun) {
                            registeredOrGotEventsThisRun = true;
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.onReadyToDisplay(false);
                                }
                            });
                        }
                    }
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, e.getMessage(), e);
                    ZLog.logException(e);
                    // Retry without backoff, since it's already been a while
                } catch (IOException e) {
                    if (request.aborting) {
                        Log.i(ASYNC_GET_EVENTS, "Thread aborted");
                        return;
                    } else {
                        backoff(e);
                    }
                } catch (JSONException e) {
                    backoff(e);
                }
                Thread.sleep(interval);
            }
        } catch (Exception e) {
            ZLog.logException(e);
        }
    }

    /**
     * Handles any event returned by the server that we care about.
     */
    private void processEvents(JSONArray events) {
        // In task thread
        try {
            StopWatch watch = new StopWatch();
            watch.start();

            ArrayList<Message> messages = new ArrayList<>();
            HashMap<String, Person> personCache = new HashMap<>();
            HashMap<String, Stream> streamCache = new HashMap<>();

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                String type = event.getString("type");

                if (type.equals("message")) {
                    Message message = new Message(this.app,
                            event.getJSONObject("message"), personCache,
                            streamCache);
                    messages.add(message);
                } else if (type.equals(POINTER)) {
                    // Keep our pointer synced with global pointer
                    app.setPointer(event.getInt(POINTER));
                }
            }

            watch.stop();
            Log.i("perf", "Processing events: " + watch.toString());

            watch.reset();
            watch.start();

            if (!messages.isEmpty()) {
                Log.i("AsyncGetEvents", "Received " + messages.size()
                        + " messages");
                Message.createMessages(app, messages);
                processMessages(messages);
            }

            watch.stop();
            Log.i("perf", "Inserting event messages: " + watch.toString());

        } catch (JSONException e) {
            ZLog.logException(e);
        }
    }

    /**
     * Add messages to the list {@link com.zulip.android.activities.MessageListFragment} which are already added to the database
     * @param messages List of messages to be added
     */
    private void processMessages(final ArrayList<Message> messages) {
        // In task thread
        int lastMessageId = messages.get(messages.size() - 1).getID();
        MessageRange.updateNewMessagesRange(app, lastMessageId);

        if (!calledFromWidget) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.onNewMessages(messages.toArray(new Message[messages.size()]));
                }
            });
        } else {
            if (!messages.isEmpty()) {
                //Reload Widget on new messages
                Log.d(TAG, "New messages recieved for calledFromWidget: " + messages.size());
                final Intent refreshIntent = new Intent(app, ZulipWidget.class);
                refreshIntent.setAction(ZulipWidget.WIDGET_REFRESH);
                app.startActivity(refreshIntent);
            }
        }
    }

}
