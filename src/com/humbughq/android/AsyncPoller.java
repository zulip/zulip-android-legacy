package com.humbughq.android;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

class AsyncPoller extends HumbugAsyncWebGet {

    public AsyncPoller(HumbugActivity humbugActivity) {
        super(humbugActivity);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        try {
            JSONArray objects = new JSONArray(result);
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
        if (this.that.suspended) {
            Log.i("poll", "suspended, dying");
            return;
        }
        this.that.current_poll = new AsyncPoller(this.that);
        this.that.current_poll.execute(HumbugActivity.SERVER_URI);
    }
}