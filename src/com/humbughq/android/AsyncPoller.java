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
        Log.v("poll", "longpolling started");
    }

    public final void execute(int anchor, int before, int after) {
        this.setProperty("anchor", anchor + "");
        this.setProperty("num_before", before + "");
        this.setProperty("num_after", after + "");
        this.execute("api/v1/get_old_messages");
        Log.v("poll", "get_old_messages called");
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
            } catch (NullPointerException e) {
                Log.e("poll", "No data returned?");
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
        Log.v("poll", "Longpolling finished.");

        if (receivedMessages != null) {
            Log.v("poll", "Processing messages received.");
            try {
                this.context.adapter.addAll(receivedMessages);
            } catch (NoSuchMethodError e) {
                for (Message message : receivedMessages) {
                    this.context.adapter.add(message);
                }
            }
        } else {
            Log.v("poll", "No messages returned.");
        }
        if (updatePointer) {
            Log.v("poll", "Starting AsyncPointerUpdate");
            (new AsyncPointerUpdate(this.context)).execute();
        }
        if (this.continuePolling) {
            Log.v("poll", "Starting new longpoll.");
            this.context.current_poll = new AsyncPoller(this.context, true,
                    false);
            this.context.current_poll.execute();
        }

        callback.onTaskComplete(result);
    }
}
