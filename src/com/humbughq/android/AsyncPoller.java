package com.humbughq.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPoller extends HumbugAsyncPushTask {

    private Message[] receivedMessages;
    private boolean continuePolling;
    private boolean updatePointer;

    public AsyncPoller(HumbugActivity humbugActivity, boolean continuePolling,
            boolean updatePointer) {
        super(humbugActivity);
        this.continuePolling = continuePolling;
        this.updatePointer = updatePointer;
    }

    public final void execute() {
        this.execute("api/v1/get_messages");
    }

    public final void execute(int anchor, String direction, int number) {
        this.setProperty("start", anchor + "");
        this.setProperty("which", direction + "");
        this.setProperty("number", number + "");
        this.execute("api/v1/get_old_messages");
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
                    Message message = new Message(context,
                            objects.getJSONObject(i));
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
        return null; // since onPostExecute doesn't use the result
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (receivedMessages != null) {
            this.context.adapter.addAll(receivedMessages);
        }
        if (updatePointer) {
            (new AsyncPointerUpdate(this.context)).execute();
        }
        if (this.continuePolling) {
            this.context.current_poll = new AsyncPoller(this.context, true,
                    false);
            this.context.current_poll.execute();
        }

        callback.onTaskComplete(result);
    }
}
