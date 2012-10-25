package com.humbughq.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPoller extends HumbugAsyncPushTask {

    private Message[] receivedMessages;
    private boolean shouldUpdatePointer;

    public AsyncPoller(HumbugActivity humbugActivity,
            boolean shouldUpdatePointer) {
        super(humbugActivity);
        this.shouldUpdatePointer = shouldUpdatePointer;
    }

    public final void execute() {
        this.execute("api/v1/get_messages");
    }

    public final void execute(int first, int last) {
        this.setProperty("first", first + "");
        this.setProperty("last", last + "");
        this.execute("api/v1/get_messages");
    }

    public final void fetchInitial() {
        this.execute(-1, -1);
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
                    this.context.messageIndex.append(message.getID(), message);

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

        if (receivedMessages != null) {
            this.context.adapter.addAll(receivedMessages);
        }
        if (shouldUpdatePointer) {
            (new AsyncPointerUpdate(this.context)).execute();
        }
        if (this.context.suspended) {
            Log.i("poll", "suspended, dying");
            return;
        }
        this.context.current_poll = new AsyncPoller(this.context, false);
        this.context.current_poll.execute();
    }
}