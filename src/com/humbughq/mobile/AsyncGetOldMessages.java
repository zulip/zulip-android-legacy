package com.humbughq.mobile;

import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.j256.ormlite.dao.Dao;

public class AsyncGetOldMessages extends HumbugAsyncPushTask {
    HumbugActivity activity;
    private Message[] receivedMessages;
    HumbugActivity.LoadPosition position;

    public AsyncGetOldMessages(HumbugActivity humbugActivity) {
        super(humbugActivity.app);
        activity = humbugActivity;
    }

    /**
     * Get messages surrounding a specified anchor message ID, inclusive of both
     * endpoints and the anchor.
     * 
     * @param anchor
     *            Message ID of the message to fetch around
     * @param before
     *            Number of messages after the anchor to return
     * @param after
     *            Number of messages before the anchor to return
     */
    public final void execute(int anchor, HumbugActivity.LoadPosition pos,
            int before, int after) {
        this.setProperty("anchor", Integer.toString(anchor));
        this.setProperty("num_before", Integer.toString(before));
        this.setProperty("num_after", Integer.toString(after));
        this.setProperty("apply_markdown", "false");
        // We don't support narrowing at all, so always specify we're
        // unnarrowed.
        this.setProperty("narrow", "{}");
        position = pos;
        execute("GET", "v1/messages");
    }

    @Override
    protected String doInBackground(String... api_path) {
        String result = super.doInBackground(api_path);

        if (result != null) {
            try {
                JSONObject response = new JSONObject(result);
                JSONArray objects = response.getJSONArray("messages");

                Dao<Message, Integer> messages = this.app.getDatabaseHelper()
                        .getDao(Message.class);

                receivedMessages = new Message[objects.length()];

                for (int i = 0; i < objects.length(); i++) {
                    Message message = new Message(this.app,
                            objects.getJSONObject(i));

                    receivedMessages[i] = message;
                    messages.createOrUpdate(message);
                }
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("poll", "No data returned?");
                e.printStackTrace();
            } catch (SQLException e) {
                // Awkward. (TODO)
                e.printStackTrace();
            }
        } else {
            Log.i("poll", "got nothing from the server");
        }
        return null; // since onPostExecute doesn't use the String result
    }

    @Override
    protected void onPostExecute(String result) {
        if (receivedMessages != null) {
            Log.v("poll", "Processing messages received.");
            activity.onMessages(receivedMessages, position);
        } else {
            Log.v("poll", "No messages returned.");
        }
        callback.onTaskComplete(result);
    }
}
