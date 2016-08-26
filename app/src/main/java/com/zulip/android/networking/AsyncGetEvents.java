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
import com.zulip.android.networking.response.events.EventsBranch;
import com.zulip.android.networking.response.events.GetEventResponse;
import com.zulip.android.networking.response.events.MessageWrapper;
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
                        }
                        catch(Exception e) {
                            ZLog.logException(e);
                        }
                    }
                    //MUST UPDATE AFTER SUBSCRIPTIONS ARE STORED IN DB
                    app.addToMutedTopics(response.getMutedTopics());

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
                            personDao.createOrUpdate(person);
                        }
                        catch(Exception e) {
                            ZLog.logException(e);
                        }
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

                    retrofit2.Response<GetEventResponse> eventResponse = app.getZulipServices()
                            .getEvents(registeredOrGotEventsThisRun ? null : true, app.getLastEventId(), app.getEventQueueId())
                            .execute();

                    GetEventResponse body = eventResponse.body();
                    if (!eventResponse.isSuccessful()) {
                        if (eventResponse.code() == 400 && ((body != null && body.getMsg().contains("Bad event queue id"))
                                || eventResponse.message().contains("too old"))) {
                            // Queue dead. Register again.
                            Log.w(ASYNC_GET_EVENTS, "Queue dead");
                            app.setEventQueueId(null);
                            continue;
                        }

                        backoff(null);
                    } else {

                        if (body.getEvents().size() > 0) {
                            this.processEvents(body);
                            app.setLastEventId(body.getEvents().get(body.getEvents().size() - 1).getId());
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
     * @param events
     */
    private void processEvents(GetEventResponse events) {
        // In task thread
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
     * @param messages List of messages to be added
     */
    private void processMessages(final List<Message> messages) {
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
