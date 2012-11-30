package com.humbughq.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPoller extends HumbugAsyncPushTask {

    private Message[] receivedMessages;
    private boolean continuePolling;

    /**
     * Initialises an AsyncPoller object and sets execution defaults.
     * 
     * @param humbugActivity
     *            The calling Activity.
     * @param continuePolling
     *            Whether to start up a new AsyncPoller task
     * @param updatePointer
     */
    public AsyncPoller(HumbugActivity humbugActivity, boolean continuePolling) {
        super(humbugActivity);
        this.continuePolling = continuePolling;
    }

    /**
     * Default longpolling which will instantly return once messages are
     * available for consumption which were sent after this method is called.
     * 
     * You probably never want to use this method.
     */
    public final void execute() {
        this.execute("api/v1/get_messages");
        Log.v("poll", "longpolling started");
    }

    /**
     * Longpoll for messages after a specified last received message.
     * 
     * @param lastMessage
     *            Last message received. More specifically, longpolling will
     *            return once messages after this message ID are available for
     *            consumption.
     */
    public final void execute(int lastMessage) {
        this.setProperty("last", lastMessage + "");
        this.execute("api/v1/get_messages");
        Log.v("poll", "longpolling started from " + lastMessage);
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
    public final void execute(int anchor, int before, int after) {
        this.setProperty("anchor", anchor + "");
        this.setProperty("num_before", before + "");
        this.setProperty("num_after", after + "");
        // We don't support narrowing at all, so always specify we're
        // unnarrowed.
        this.setProperty("narrow", "{}");
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
                    Message message = new Message(context.you,
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
        return null; // since onPostExecute doens't use the result
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
                /*
                 * Older versions of Android do not have .addAll, so we fall
                 * back to manually looping here.
                 */
                for (Message message : receivedMessages) {
                    this.context.adapter.add(message);
                }
            }
        } else {
            Log.v("poll", "No messages returned.");
        }
        if (this.continuePolling) {
            Log.v("poll", "Starting new longpoll.");
            this.context.current_poll = new AsyncPoller(this.context, true);
            // Start polling from the last received message ID
            this.context.current_poll.execute((int) context.adapter
                    .getItemId(context.adapter.getCount() - 1));
        }

        callback.onTaskComplete(result);
    }
}
