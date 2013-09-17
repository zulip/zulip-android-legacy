package com.humbughq.mobile;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

public class AsyncGetEvents extends Thread {
    HumbugActivity activity;
    ZulipApp app;

    Handler onRegisterHandler;
    Handler onEventsHandler;
    HTTPRequest request;

    AsyncGetEvents that = this;
    int failures = 0;
    boolean registeredOrGotEventsThisRun;

    public AsyncGetEvents(HumbugActivity humbugActivity) {
        super();
        app = humbugActivity.app;
        activity = humbugActivity;
        request = new HTTPRequest(app);
    }

    public void start() {
        registeredOrGotEventsThisRun = false;

        if (this.app.getPointer() > 0 && this.activity.currentRange == null) {
            this.activity.populateCurrentRange();
        }

        onRegisterHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                that.onRegister((JSONObject) msg.obj);
            }
        };

        onEventsHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                JSONArray events;
                try {
                    events = ((JSONObject) msg.obj).getJSONArray("events");
                    for (int i = 0; i < events.length(); i++) {
                        that.onEvent(events.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        super.start();
    }

    public void abort() {
        // TODO: does this have race conditions? (if the thread is not in a
        // request when called)
        Log.i("asyncGetEvents", "Interrupting thread");
        request.abort();
    }

    private void backoff(Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
        failures += 1;
        long backoff = (long) (Math.exp(failures / 2.0) * 1000);
        Log.e("asyncGetEvents", "Failure " + failures + ", sleeping for "
                + backoff);
        SystemClock.sleep(backoff);
    }

    private void register() throws JSONException, IOException {
        request.setProperty("apply_markdown", "false");
        JSONObject response = new JSONObject(request.execute("POST",
                "v1/register"));

        registeredOrGotEventsThisRun = true;
        app.setEventQueueId(response.getString("queue_id"));
        app.setLastEventId(response.getInt("last_event_id"));

        onRegisterHandler.obtainMessage(0, response).sendToTarget();
    }

    public void run() {
        try {
            while (true) {
                try {
                    request.clearProperties();
                    if (app.getEventQueueId() == null) {
                        register();
                    }
                    request.setProperty("queue_id", app.getEventQueueId());
                    request.setProperty("last_event_id",
                            "" + app.getLastEventId());
                    if (registeredOrGotEventsThisRun == false) {
                        request.setProperty("dont_block", "true");
                    }
                    JSONObject response = new JSONObject(request.execute("GET",
                            "v1/events"));

                    JSONArray events = response.getJSONArray("events");
                    JSONObject lastEvent = events
                            .getJSONObject(events.length() - 1);

                    app.setLastEventId(lastEvent.getInt("id"));

                    onEventsHandler.obtainMessage(0, response).sendToTarget();
                    failures = 0;

                    if (registeredOrGotEventsThisRun == false) {
                        registeredOrGotEventsThisRun = true;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.onReadyToDisplay();
                            }
                        });
                    }
                } catch (HttpResponseException e) {
                    if (e.getStatusCode() == 400) {
                        String msg = e.getMessage();
                        if (msg.contains("Bad event queue id")
                                || msg.contains("too old")) {
                            // Queue dead. Register again.
                            Log.w("asyncGetEvents", "Queue dead");
                            app.setEventQueueId(null);
                            continue;
                        }
                    }
                    backoff(e);
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    // Retry without backoff, since it's already been a while
                } catch (IOException e) {
                    if (request.aborting) {
                        Log.i("asyncGetEvents", "Thread aborted");
                        return;
                    } else {
                        backoff(e);
                    }
                } catch (JSONException e) {
                    backoff(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onRegister(JSONObject response) {
        try {

            app.setPointer(response.getInt("pointer"));
            app.setMaxMessageId(response.getInt("max_message_id"));

            activity.populateCurrentRange();

            // Get subscriptions
            JSONArray subscriptions = response.getJSONArray("subscriptions");
            Dao<Stream, String> streamDao = this.app.getDao(Stream.class);
            Log.i("stream", "" + subscriptions.length() + " streams");

            for (int i = 0; i < subscriptions.length(); i++) {
                Stream stream = Stream.getFromJSON(app,
                        subscriptions.getJSONObject(i));
                Log.i("stream", "" + stream);

                try {
                    streamDao.createOrUpdate(stream);
                } catch (SQLException e) {
                    // This isn't totally fatal, because while a lack of
                    // stream data is depressing our app will still
                    // mostly function without it.
                    Log.e("ALP",
                            "Could not create or update stream in database.");
                    e.printStackTrace();
                }
            }
            that.activity.streamsAdapter.refresh();

            // Get people
            JSONArray people = response.getJSONArray("realm_users");
            Dao<Person, String> personDao = this.app.getDao(Person.class);
            for (int i = 0; i < people.length(); i++) {
                Person person = Person
                        .getFromJSON(app, people.getJSONObject(i));
                Log.i("person", "" + person);
                try {
                    personDao.createOrUpdate(person);
                } catch (SQLException e) {
                    Log.e("ALP",
                            "Could not create or update person in database");
                    e.printStackTrace();
                }
            }
            that.activity.peopleAdapter.refresh();

            that.activity.onReadyToDisplay();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void onEvent(JSONObject event) {
        try {
            Log.i("Event:", event.toString());
            String type = event.getString("type");

            if (type.equals("message")) {
                onMessage(event.getJSONObject("message"));
            } else if (type.equals("pointer")) {
                // Keep our pointer synced with global pointer
                app.setPointer(event.getInt("pointer"));
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void onMessage(JSONObject m) throws JSONException {
        final Message message = new Message(this.app, m);
        try {
            Dao<Message, Integer> messages = this.app.getDao(Message.class);
            messages.createOrUpdate(message);
            if (this.activity.currentRange.high <= message.getID()) {
                this.app.getDao(MessageRange.class).createOrUpdate(
                        this.activity.currentRange);
            }
        } catch (SQLException e) {
            // Awkward. (TODO)
            e.printStackTrace();
        }
        Message[] messages = { message };
        this.activity.onMessages(messages, HumbugActivity.LoadPosition.NEW);
    }
}