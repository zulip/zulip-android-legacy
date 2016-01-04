package com.zulip.android;

import android.content.Context;
import android.os.*;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends a status update and fetches updates for other users.
 */
public class AsyncStatusUpdate extends ZulipAsyncPushTask {

    private final Context context;

    /**
     * Declares a new HumbugAsyncPushTask, passing the activity as context.
     * 
     * @param activity
     */
    public AsyncStatusUpdate(ZulipActivity activity) {
        super(activity.app);
        this.context = activity;

        setProperty("status", "active");
    }

    public final void execute() {
        execute("POST", "v1/users/me/presence");
    }

    /**
     * Choose presence object. This will return the latest active presence, then
     * the latest away presence available.
     */
    private JSONObject chooseLatestPresence(JSONObject person)
            throws JSONException {
        Iterator keys = person.keys();
        JSONObject latestPresence = null;
        while (keys.hasNext()) {
            String key = (String) keys.next();
            JSONObject presence = person.getJSONObject(key);
            long timestamp = presence.getLong("timestamp");
            String status = presence.getString("status");
            if (latestPresence == null) {
                // first presence
                latestPresence = presence;
            } else {
                String latestStatus = latestPresence.getString("status");
                if (latestStatus == null) {
                    Log.wtf("statusUpdate",
                            "Received presence information with no status");
                } else if (status == null) {
                    Log.wtf("statusUpdate",
                            "Received presence information with no status");
                } else {
                    if (status.equals(PresenceType.ACTIVE.toString())) {
                        if (latestStatus.equals(PresenceType.ACTIVE.toString())) {
                            if (latestPresence.getLong("timestamp") < timestamp) {
                                latestPresence = presence;
                            }
                        } else {
                            // found an active status which overrides the idle
                            // status
                            latestPresence = presence;
                        }
                    } else if (status.equals(PresenceType.IDLE.toString())) {
                        if (latestStatus.equals(PresenceType.IDLE.toString())) {
                            if (latestPresence.getLong("timestamp") < timestamp) {
                                latestPresence = presence;
                            }
                        }
                    }
                }
            }
        }
        return latestPresence;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.getString("result").equals("success")) {

                    ConcurrentHashMap<String, Presence> presenceLookup = this.app.presences;
                    presenceLookup.clear();

                    JSONObject presences = obj.getJSONObject("presences");
                    long serverTimestamp = (long) obj
                            .getDouble("server_timestamp");
                    if (presences != null) {
                        Iterator emailIterator = presences.keys();
                        while (emailIterator.hasNext()) {
                            String email = (String) emailIterator.next();
                            JSONObject person = presences.getJSONObject(email);

                            // iterate through the devices providing updates and
                            // use the status of the latest one
                            JSONObject latestPresenceObj = chooseLatestPresence(person);
                            if (latestPresenceObj != null) {
                                long age = serverTimestamp
                                        - latestPresenceObj
                                                .getLong("timestamp");
                                String status = latestPresenceObj
                                        .getString("status");
                                String client = latestPresenceObj
                                        .getString("client");

                                PresenceType statusEnum;
                                if (status == null) {
                                    statusEnum = null;
                                } else if (status.equals(PresenceType.ACTIVE
                                        .toString())) {
                                    statusEnum = PresenceType.ACTIVE;
                                } else if (status.equals(PresenceType.IDLE
                                        .toString())) {
                                    statusEnum = PresenceType.IDLE;
                                } else {
                                    statusEnum = null;
                                }
                                Presence presence = new Presence(age, client,
                                        statusEnum);
                                presenceLookup.put(email, presence);
                            }
                        }
                    }

                    callback.onTaskComplete(result);

                    return;
                }
            } catch (JSONException e) {
                ZLog.logException(e);
            }
        }
        Toast.makeText(context, "Unknown error", Toast.LENGTH_LONG).show();
        Log.wtf("statusUpdate",
                "An exception was thrown or we got a failed response from the server for status updates.");
    }
}
