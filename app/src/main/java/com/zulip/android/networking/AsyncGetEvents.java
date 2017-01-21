package com.zulip.android.networking;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.activities.LoginActivity;
import com.zulip.android.activities.ZulipActivity;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageRange;
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;
import com.zulip.android.networking.response.UserConfigurationResponse;
import com.zulip.android.networking.response.events.EventsBranch;
import com.zulip.android.networking.response.events.GetEventResponse;
import com.zulip.android.networking.response.events.MessageWrapper;
import com.zulip.android.networking.response.events.SubscriptionWrapper;
import com.zulip.android.util.MutedTopics;
import com.zulip.android.util.TypeSwapper;
import com.zulip.android.util.ZLog;
import com.zulip.android.widget.ZulipWidget;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A background task which asynchronously fetches the updates from the server.
 * The run method {@link #run()} which has an infinite loop and keeps fetching the latest updates
 * and handles the events appropriately.
 * If the user is not registered to a queue this registers {@link #register()} for a new lastEventId and queueID
 * <p>
 * lastEventId {@link ZulipApp#lastEventId} which is used to fetch after this ID events from the server.
 */
public class AsyncGetEvents extends Thread {
    private static final String TAG = "AsyncGetEvents";
    private static final String ASYNC_GET_EVENTS = "asyncGetEvents";

    private boolean keepThisRunning = true;
    private HTTPRequest request;

    private int failures = 0;
    private boolean registeredOrGotEventsThisRun;
    private MutedTopics mMutedTopics;
    private ZulipApp app;
    private ZulipActivity mActivity;
    private int mInterval = 1000;

    public AsyncGetEvents(ZulipActivity activity) {
        super();
        mActivity = activity;
        init();
    }

    public AsyncGetEvents(int interval) {
        super();
        mInterval = interval;
        init();
    }

    private void init() {
        app = ZulipApp.get();
        mMutedTopics = MutedTopics.get();
        request = new HTTPRequest(app);
    }

    public void start() {
        registeredOrGotEventsThisRun = false;
        if (mActivity != null) {
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
        if (response.isSuccessful()) {
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
                        try {
                            streamDao.createOrUpdate(stream);
                        } catch (Exception e) {
                            ZLog.logException(e);
                        }
                    }
                    //MUST UPDATE AFTER SUBSCRIPTIONS ARE STORED IN DB
                    mMutedTopics.addToMutedTopics(response.getMutedTopics());

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
                        try {
                            // Ormlite uses a query-by-id to see if it needs to update
                            // or create a new row using createOrUpdate()
                            // Hence, the object passed should always have an id.
                            if (Person.getByEmail(app, person.getEmail()) == null) {
                                personDao.create(person);
                            } else {
                                personDao.update(person);
                            }
                        } catch (Exception e) {
                            ZLog.logException(e);
                        }
                    }
                    return null;
                }
            });

            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.getPeopleAdapter().refresh();
                        mActivity.onReadyToDisplay(true);
                        mActivity.checkAndSetupStreamsDrawer();
                    }
                });
            }
        } catch (SQLException e) {
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

                    retrofit2.Response<GetEventResponse> eventResponse = app.getZulipServices()
                            .getEvents(registeredOrGotEventsThisRun ? null : true, app.getLastEventId(), app.getEventQueueId())
                            .execute();

                    GetEventResponse body = eventResponse.body();

                    if (!eventResponse.isSuccessful()) {

                        NetworkError errorBody = eventResponse.errorBody() != null ?
                                app.getGson().fromJson(eventResponse.errorBody().string(), NetworkError.class)
                                : null;

                        if (eventResponse.code() == 400 && ((errorBody != null && errorBody.getMsg().contains("Bad event queue id"))
                                || eventResponse.message().contains("too old"))) {
                            // Queue dead. Register again.
                            Log.w(ASYNC_GET_EVENTS, "Queue dead");
                            app.setEventQueueId(null);
                            continue;
                        } else if (eventResponse.code() == 401) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(app.getBaseContext(), R.string.force_logged_out, Toast.LENGTH_LONG).show();
                                    app.logOut();
                                    Intent i = new Intent(app, LoginActivity.class);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    app.startActivity(i);
                                }
                            });
                            break;
                        }

                        backoff(null);
                    } else {

                        if (body.getEvents().size() > 0) {
                            this.processEvents(body);
                            app.setLastEventId(body.getEvents().get(body.getEvents().size() - 1).getId());
                            failures = 0;
                        }

                        if (!registeredOrGotEventsThisRun && mActivity != null) {
                            registeredOrGotEventsThisRun = true;
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mActivity.onReadyToDisplay(false);
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
                Thread.sleep(mInterval);
            }
        } catch (Exception e) {
            ZLog.logException(e);
        }
    }

    /**
     * Handles any event returned by the server that we care about.
     *
     * @param events sent by server
     */
    private void processEvents(GetEventResponse events) {
        // In task thread
        List<EventsBranch> subscriptions = events.getEventsOfBranchType(EventsBranch.BranchType.SUBSCRIPTIONS);

        if (!subscriptions.isEmpty()) {
            Log.i("AsyncGetEvents", "Received " + subscriptions.size()
                    + " streams event");
            processSubsciptions(subscriptions);
        }

        // get messages from events
        List<Message> messages = events.getEventsOf(EventsBranch.BranchType.MESSAGE, new TypeSwapper<MessageWrapper, Message>() {
            @Override
            public Message convert(MessageWrapper messageWrapper) {
                return messageWrapper.getMessage();
            }
        });
        if (!messages.isEmpty()) {
            Log.i("AsyncGetEvents", "Received " + messages.size()
                    + " messages");
            Message.createMessages(app, messages);
            processMessages(messages);
        }

    }

    /**
     * Add messages to the list {@link com.zulip.android.activities.MessageListFragment} which are already added to the database
     *
     * @param messages List of messages to be added
     */
    private void processMessages(final List<Message> messages) {
        // In task thread
        int lastMessageId = messages.get(messages.size() - 1).getID();
        MessageRange.updateNewMessagesRange(app, lastMessageId);

        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.onNewMessages(messages.toArray(new Message[messages.size()]));
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

    private void processSubsciptions(List<EventsBranch> subscriptionWrapperList) {
        RuntimeExceptionDao<Stream, Object> streamDao = app
                .getDao(Stream.class);

        for (EventsBranch wrapper : subscriptionWrapperList) {
            SubscriptionWrapper subscriptionwrapper = (SubscriptionWrapper) wrapper;
            List<Stream> streams = subscriptionwrapper.getStreams();
            if (subscriptionwrapper.getOperation().equalsIgnoreCase(SubscriptionWrapper.OPERATION_ADD)) {

                for (Stream stream : streams) {
                    stream.getParsedColor();
                    stream.setSubscribed(true);
                    try {
                        streamDao.createOrUpdate(stream);
                    } catch (Exception e) {
                        ZLog.logException(e);
                    }
                }
            } else if (subscriptionwrapper.getOperation().equalsIgnoreCase(SubscriptionWrapper.OPERATION_UPDATE)) {
                Stream stream = subscriptionwrapper.getUpdatedStream();
                try {
                    streamDao.createOrUpdate(stream);
                } catch (Exception e) {
                    ZLog.logException(e);
                }
            } else if (subscriptionwrapper.getOperation().equalsIgnoreCase(SubscriptionWrapper.OPERATION_REMOVE)) {
                for (Stream stream : streams) {
                    stream.getParsedColor();
                    stream.setSubscribed(false);
                    try {
                        streamDao.createOrUpdate(stream);
                    } catch (Exception e) {
                        ZLog.logException(e);
                    }
                }
            } else {
                Log.d("AsyncEvents", "unknown operation for subscription type event");
            }
        }

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.checkAndSetupStreamsDrawer();
            }
        });
    }
}
