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

    public final void execute() {
        this.shouldHaveReceivedPointer = true;
        execute("GET", "v1/users/me");
    }

    public final void execute(int newPointer) {
        this.shouldHaveReceivedPointer = false;
        this.setProperty("client_id", this.app.client_id);
        this.setProperty("pointer", Integer.toString(newPointer));
        execute("PUT", "v1/users/me/pointer");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (this.shouldHaveReceivedPointer && result != null) {
            try {
                final int pointer = (new JSONObject(result)).getInt("pointer");
                this.app.client_id = (new JSONObject(result))
                        .getString("client_id");
                Log.i("pointer", "got from server as " + pointer);

                Message message = this.context.messageIndex.get(pointer);
                if (message == null) {
                    /*
                     * We're missing the pointer in the fetched message view!
                     * 
                     * This is totally okay, because if AsyncPointerUpdate is
                     * run at the start of the activity there will be no
                     * messages loaded at all. In any case, we now retrieve
                     * messages before and after the pointer.
                     */
                    Log.d("pointer", pointer + " not found in message list.");

                    this.context.current_poll = new AsyncPoller(this.context,
                            true);

                    /*
                     * We need to hook into the end of the task to update the
                     * pointer.
                     * 
                     * Due to
                     * 
                     * https://groups.google.com/d/topic/android-developers
                     * /EnyldBQDUwE/discussion
                     * 
                     * we need to post an event to set the selection after other
                     * code has run to handle the adapter's dataset being
                     * invalidated.
                     */
                    this.context.current_poll
                            .setCallback(new AsyncTaskCompleteListener() {
                                @Override
                                public void onTaskComplete(String result) {

                                    final Message message = context.messageIndex
                                            .get(pointer);
                                    context.listView.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            context.listView
                                                    .setSelection(context.adapter
                                                            .getPosition(message));
                                        }
                                    });
                                }
                            });

                    this.context.current_poll.execute(pointer, 100, 100);

                } else {
                    // Set the pointer and kick off a poll if one isn't already
                    // running.
                    this.context.listView.setSelection(this.context.adapter
                            .getPosition(message));
                    if (this.context.current_poll.isCancelled()
                            || this.context.current_poll.getStatus() == AsyncPoller.Status.FINISHED) {
                        this.context.current_poll = new AsyncPoller(
                                this.context, true);
                        this.context.current_poll
                                .execute((int) this.context.adapter
                                        .getItemId(this.context.adapter
                                                .getCount() - 1) + 1);
                    }
                }

                callback.onTaskComplete(result);
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            }
        }
    }
}
