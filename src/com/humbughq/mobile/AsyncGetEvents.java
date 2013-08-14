package com.humbughq.mobile;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

public class AsyncGetEvents extends Thread {
    HumbugActivity activity;
    ZulipApp app;

    Handler onRegisterHandler;
    Handler onEventsHandler;
    HTTPRequest request;

    AsyncGetEvents that = this;

    public AsyncGetEvents(HumbugActivity humbugActivity) {
        super();
        app = humbugActivity.app;
        activity = humbugActivity;
        request = new HTTPRequest(app);
    }

    public void start() {

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

    public void run() {
        try {
            request.setProperty("apply_markdown", "false");
            JSONObject response = new JSONObject(request.execute("POST",
                    "v1/register"));

            String eventQueueId = response.getString("queue_id");
            int lastEventId = response.getInt("last_event_id");

            onRegisterHandler.obtainMessage(0, response).sendToTarget();

            while (true) {
                request.clearProperties();
                request.setProperty("queue_id", eventQueueId);
                request.setProperty("last_event_id", "" + lastEventId);
                response = new JSONObject(request.execute("GET", "v1/events"));

                JSONArray events = response.getJSONArray("events");
                JSONObject lastEvent = events
                        .getJSONObject(events.length() - 1);
                lastEventId = lastEvent.getInt("id");

                onEventsHandler.obtainMessage(0, response).sendToTarget();
            }

        } catch (JSONException e) {
            // TODO: Some means of automatically retrying / restarting
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            if (request.aborting) {
                Log.i("asyncGetEvents", "Thread aborted");
                return;
            }
            e.printStackTrace();
        }
    }

    protected void onRegister(JSONObject response) {
        try {

            app.pointer = response.getInt("pointer");
            app.max_message_id = response.getInt("max_message_id");

            // Get subscriptions
            JSONArray subscriptions = response.getJSONArray("subscriptions");
            Dao<Stream, String> streamDao = this.app.getDao(Stream.class);
            Log.i("stream", "" + subscriptions.length() + " streams");

            for (int i = 0; i < subscriptions.length(); i++) {
                Stream stream = new Stream(subscriptions.getJSONObject(i));
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

            activity.onRegister();
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
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void onMessage(JSONObject m) throws JSONException {
        Message message = new Message(this.app, m);
        try {
            Dao<Message, Integer> messages = this.app.getDao(Message.class);
            messages.createOrUpdate(message);
        } catch (SQLException e) {
            // Awkward. (TODO)
            e.printStackTrace();
        }
        this.activity.onMessage(message);
    }
}