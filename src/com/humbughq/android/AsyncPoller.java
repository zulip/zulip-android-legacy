package com.humbughq.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPoller extends HumbugAsyncPushTask {

    private Message[] receivedMessages;

    public AsyncPoller(HumbugActivity humbugActivity) {
        super(humbugActivity);
    }

    public final void execute() {
        this.execute("api/v1/get_messages");
    }

    public final void execute(int first, int last) {
        this.setProperty("first", first + "");
        this.setProperty("last", last + "");
        this.execute("api/v1/get_messages");
    }

    @Override
    protected String doInBackground(String... api_path) {
        String result = super.doInBackground(api_path);

        if (result != null) {
            try {
                JSONObject response = new JSONObject(result);
                JSONArray objects = response.getJSONArray("messages");

                receivedMessages = new Message[objects.length()];

                for (int i = 0; i < objects.length(); i++) {
                    Log.i("json-iter", "" + i);
                    Message message = new Message(objects.getJSONObject(i));
                    /*
                     * Add to the local message array and the global ArrayList
                     * of all messages.
                     * 
                     * We do this because we want to process JSON here, but do
                     * manipulation of the UI on the UI thread.
                     */
                    receivedMessages[i] = message;
                    this.that.messages.add(message);
                }
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            }
        } else {
            Log.i("poll", "got nothing from the server");
        }
        return null; // since onPostExecute doens't use the result
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (receivedMessages == null) {
            return;
        }
        for (Message message : receivedMessages) {
            this.that.tilepanel.addView(this.that.renderStreamMessage(message));
        }

        if (this.that.suspended) {
            Log.i("poll", "suspended, dying");
            return;
        }
        this.that.current_poll = new AsyncPoller(this.that);
        this.that.current_poll.execute();
    }
}