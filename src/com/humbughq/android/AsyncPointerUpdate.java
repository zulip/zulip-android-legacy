package com.humbughq.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPointerUpdate extends HumbugAsyncPushTask {

    public AsyncPointerUpdate(HumbugActivity humbugActivity) {
        super(humbugActivity);
    }

    public final void execute() {
        execute("api/v1/get_profile");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            int pointer = (new JSONObject(result)).getInt("pointer");
            Log.i("pointer", "got from server as " + pointer);

            Message message = this.context.messageIndex.get(pointer);
            if (message == null) {
                /*
                 * We're missing the pointer in the fetched message view!
                 * 
                 * This is totally okay, because if AsyncPointerUpdate is run at
                 * the start of the activity there will be no messages loaded at
                 * all. In any case, we now retrieve messages before and after
                 * the pointer.
                 */
                Log.d("pointer", pointer + " not found in message list.");

                this.context.current_poll = new AsyncPoller(this.context, true,
                        true);

                this.context.current_poll.execute(pointer, "around", 1000);

            } else {
                this.context.listView.setSelection(this.context.adapter
                        .getPosition(message));
            }

        } catch (JSONException e) {
            Log.e("json", "parsing error");
            e.printStackTrace();
        }

    }
}