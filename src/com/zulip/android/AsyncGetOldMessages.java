package com.zulip.android;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.MessageListener.LoadPosition;

public class AsyncGetOldMessages extends ZulipAsyncPushTask {
    MessageListener listener;
    public ArrayList<Message> receivedMessages;
    MessageListener.LoadPosition position;
    protected MessageRange rng;
    protected int mainAnchor;
    protected NarrowFilter filter;
    private int before;
    private int afterAnchor;
    private int after;
    AsyncGetOldMessages that = this;

    boolean recursedAbove = false;
    boolean recursedBelow = false;

    boolean noFurtherMessages = false;
    int rangeHigh = -2;

    public AsyncGetOldMessages(MessageListener listener) {
        super(ZulipApp.get());
        this.listener = listener;
        rng = null;
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
    public final void execute(int anchor, LoadPosition pos, int before,
            int after, NarrowFilter filter) {
        this.mainAnchor = anchor;
        this.before = before;
        this.afterAnchor = mainAnchor + 1;
        this.after = after;
        this.filter = filter;
        position = pos;
        this.receivedMessages = new ArrayList<Message>();
        Log.i("AGOM", "executing " + anchor + " " + before + " " + after);
        execute("GET", "v1/messages");
    }

    @Override
    protected String doInBackground(String... params) {
        // Lets see whether we have this cached already
        final RuntimeExceptionDao<MessageRange, Integer> messageRangeDao = app
                .getDao(MessageRange.class);
        RuntimeExceptionDao<Message, Object> messageDao = app
                .getDao(Message.class);
        messageDao.setObjectCache(true);
        try {
            if (rng == null) {
                // We haven't been passed a range, see if we have a range cached
                MessageRange protoRng = MessageRange.getRangeContaining(
                        mainAnchor, messageRangeDao);
                Log.i("AGOM", "rng retreived");
                if (protoRng != null) {

                    StopWatch watch = new StopWatch();
                    watch.start();

                    // We found a range, lets get relevant messages from the
                    // cache
                    rng = protoRng;
                    rangeHigh = rng.high;

                    // we order by descending so that our limit query DTRT

                    Where<Message, Object> filteredWhere = messageDao
                            .queryBuilder().orderBy(Message.ID_FIELD, false)
                            .limit((long) before + 1).where();
                    if (filter != null) {
                        filter.modWhere(filteredWhere);
                        filteredWhere.and();
                    }
                    List<Message> lowerCachedMessages = filteredWhere
                            .le(Message.ID_FIELD, mainAnchor).and()
                            .ge(Message.ID_FIELD, rng.low).query();
                    // however because of this we need to flip the ordering
                    Collections.reverse(lowerCachedMessages);

                    filteredWhere = messageDao.queryBuilder()
                            .orderBy(Message.ID_FIELD, true)
                            .limit((long) after).where();
                    if (filter != null) {
                        filter.modWhere(filteredWhere);
                        filteredWhere.and();
                    }
                    List<Message> upperCachedMessages = filteredWhere
                            .gt("id", mainAnchor).and().le("id", rng.high)
                            .query();
                    before -= lowerCachedMessages.size();
                    // One more than size to account for missing anchor
                    after -= upperCachedMessages.size() + 1;
                    if (lowerCachedMessages.size() > 0) {
                        mainAnchor = lowerCachedMessages.get(0).getID() - 1;
                    }
                    if (upperCachedMessages.size() > 0) {
                        afterAnchor = upperCachedMessages.get(
                                upperCachedMessages.size() - 1).getID() + 1;
                    }
                    receivedMessages.addAll(lowerCachedMessages);
                    receivedMessages.addAll(upperCachedMessages);

                    watch.stop();
                    Log.i("perf",
                            "Retrieving cached messages: " + watch.toString());

                    if (lowerCachedMessages.size() > 0
                            || upperCachedMessages.size() > 0) {
                        if (before > 0) {
                            this.recurse(LoadPosition.ABOVE, before, rng,
                                    mainAnchor);
                        }

                        // Don't fetch past the max message ID because we won't
                        // find anything there. However, leave the final
                        // determination up to the UI thread, because it has a
                        // consistent view around events. With this, at
                        // worst it has to do another fetch to get the new
                        // messages.
                        if (after > 0 && rng.high != app.getMaxMessageId()) {
                            this.recurse(LoadPosition.BELOW, after, rng,
                                    afterAnchor);
                        }
                        return null;
                    }
                } else {
                    // We don't have anything cached
                    Log.w("db_gom", "No messages found in specified range!");
                }
            }
            if (fetchMessages(mainAnchor, before, after, params)) {
                if (filter == null) {
                    int lowest = receivedMessages.get(0).getID();
                    int highest = receivedMessages.get(
                            receivedMessages.size() - 1).getID();

                    // We know there are no messages between the anchor and what
                    // we received or we would have fetched them.
                    if (lowest > mainAnchor)
                        lowest = mainAnchor;
                    if (highest < mainAnchor)
                        highest = mainAnchor;

                    MessageRange.markRange(app, lowest, highest);
                }
            }
        } catch (SQLException e) {
            // Still welp.
            throw new RuntimeException(e);
        }

        return null; // since onPostExecute doesn't use the String result
    }

    protected void recurse(LoadPosition position, int amount, MessageRange rng,
            int anchor) {
        AsyncGetOldMessages task = new AsyncGetOldMessages(listener);
        task.rng = rng;
        switch (position) {
        case ABOVE:
            recursedAbove = true;
            task.execute(anchor, position, amount, 0, filter);
            break;
        case BELOW:
            task.execute(anchor, position, 0, amount, filter);
            recursedBelow = true;
            break;
        default:
            Log.wtf("AGOM", "recurse passed unexpected load position!");
            break;
        }

    }

    protected boolean fetchMessages(int anchor, int num_before, int num_after,
            String[] params) {
        this.setProperty("anchor", Integer.toString(anchor));
        this.setProperty("num_before", Integer.toString(num_before));
        this.setProperty("num_after", Integer.toString(num_after));
        this.setProperty("apply_markdown", "true");

        if (filter != null) {
            try {
                this.setProperty("narrow", filter.getJsonFilter());
            } catch (JSONException e) {
                Log.wtf("fm", e);
            }
        } else {
            this.setProperty("narrow", "{}");
        }

        StopWatch watch = new StopWatch();

        watch.start();
        String result = super.doInBackground(params);
        watch.stop();
        Log.i("perf", "net: v1/messages: " + watch.toString());

        if (result != null) {
            try {
                watch.reset();
                watch.start();
                JSONObject response = new JSONObject(result);
                watch.stop();
                Log.i("perf", "json: v1/messages: " + watch.toString());

                watch.reset();
                watch.start();
                JSONArray objects = response.getJSONArray("messages");
                ArrayList<Message> fetchedMessages = new ArrayList<Message>(
                        objects.length());

                HashMap<String, Person> personCache = new HashMap<String, Person>();
                HashMap<String, Stream> streamCache = new HashMap<String, Stream>();

                for (int i = 0; i < objects.length(); i++) {
                    Message message = new Message(this.app,
                            objects.getJSONObject(i), personCache, streamCache);
                    fetchedMessages.add(message);
                }
                watch.stop();
                Log.i("perf", "creating messages " + watch.toString());

                watch.reset();
                watch.start();
                Message.createMessages(app, fetchedMessages);
                watch.stop();
                Log.i("perf", "sqlite: messages " + watch.toString());

                if (num_after == 0) {
                    receivedMessages.addAll(0, fetchedMessages);
                } else {
                    receivedMessages.addAll(fetchedMessages);
                }

                if ((position == LoadPosition.ABOVE || position == LoadPosition.BELOW)
                        && receivedMessages.size() < num_before + num_after) {
                    noFurtherMessages = true;
                }

                return receivedMessages.size() > 0;
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                ZLog.logException(e);
            } catch (NullPointerException e) {
                Log.e("poll", "No data returned?");
                ZLog.logException(e);
            }
        } else {
            Log.i("poll", "got nothing from the server");
        }
        return false;
    }

    @Override
    protected void onPostExecute(String result) {
        if (receivedMessages != null) {
            Log.v("poll", "Processing messages received.");

            if ((position == LoadPosition.BELOW || position == LoadPosition.INITIAL)
                    && app.getMaxMessageId() == rangeHigh) {
                noFurtherMessages = true;
            }

            listener.onMessages(receivedMessages.toArray(new Message[0]),
                    position, recursedAbove, recursedBelow, noFurtherMessages);
        } else {
            listener.onMessageError(position);
            Log.v("poll", "No messages returned.");
        }
        callback.onTaskComplete(result);
    }

    protected void onCancelled(String result) {
        listener.onMessageError(position);
        callback.onTaskFailure(result);
    }
}
