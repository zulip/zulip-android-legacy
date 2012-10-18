package com.humbughq.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPoller extends HumbugAsyncPushTask {

    public AsyncPoller(HumbugActivity humbugActivity) {
        super(humbugActivity);
    }

    public final void execute() {
        this.execute("api/v1/get_messages");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null) {
            try {
                JSONObject response = new JSONObject(result);
                JSONArray objects = response.getJSONArray("messages");
                for (int i = 0; i < objects.length(); i++) {
                    Log.i("json-iter", "" + i);
                    Message message = new Message(objects.getJSONObject(i));
                    this.that.messages.add(message);

                    if (message.getType() == Message.STREAM_MESSAGE) {
                        this.that.tilepanel.addView(this.that
                                .renderStreamMessage(message));
                    }
                }
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            }
        } else {
            Log.i("poll", "got nothing from the server");
        }
        if (this.that.suspended) {
            Log.i("poll", "suspended, dying");
            return;
        }
        this.that.current_poll = new AsyncPoller(this.that);
        this.that.current_poll.execute();
    }
}