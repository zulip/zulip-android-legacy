package com.humbughq.mobile;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPointerUpdate extends HumbugAsyncPushTask {
    HumbugActivity context;
    boolean shouldHaveReceivedPointer;

    public AsyncPointerUpdate(HumbugActivity humbugActivity) {
        super(humbugActivity.app);
        context = humbugActivity;
    }

    public final void execute(int newPointer) {
        this.shouldHaveReceivedPointer = false;
        this.setProperty("pointer", Integer.toString(newPointer));
        execute("PUT", "v1/users/me/pointer");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (this.shouldHaveReceivedPointer && result != null) {
            try {
                final int pointer = (new JSONObject(result)).getInt("pointer");
                Log.i("pointer", "got from server as " + pointer);

                Message message = this.context.messageIndex.get(pointer);
                if (message != null) {
                    // Set the pointer
                    this.context.listView.setSelection(this.context.adapter
                            .getPosition(message));

                }

                callback.onTaskComplete(result);
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            }
        }
    }
}
