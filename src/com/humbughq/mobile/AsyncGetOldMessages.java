package com.humbughq.mobile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.Where;

public class AsyncGetOldMessages extends HumbugAsyncPushTask {
    HumbugActivity activity;
    private ArrayList<Message> receivedMessages;
    HumbugActivity.LoadPosition position;
    protected MessageRange rng;
    protected int mainAnchor;
    private int before;
    private int afterAnchor;
    private int after;
    AsyncGetOldMessages that = this;

    public AsyncGetOldMessages(HumbugActivity humbugActivity) {
        super(humbugActivity.app);
        activity = humbugActivity;
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
    public final void execute(int anchor, HumbugActivity.LoadPosition pos,
            int before, int after) {
        this.mainAnchor = anchor;
        this.before = before;
        this.afterAnchor = mainAnchor + 1;
        this.after = after;
        position = pos;
        this.receivedMessages = new ArrayList<Message>();
        execute("GET", "v1/messages");
    }

    @Override
    protected String doInBackground(String... params) {
        // Lets see whether we have this cached already
        final Dao<MessageRange, Integer> messageRangeDao = this.activity.app
                .getDao(MessageRange.class);
        Dao<Message, Integer> messageDao = this.activity.app
                .getDao(Message.class);
        try {
            if (rng == null) {
                // We haven't been passed a range, see if we have a range cached
                MessageRange protoRng = MessageRange.getRangeContaining(
                        mainAnchor, messageRangeDao);
                Log.e("AGOM", "rng retreived");
                if (protoRng != null) {
                    // We found a range, lets get relevant messages from the
                    // cache
                    rng = protoRng;
                    // we order by decending so that our limit query DTRT
                    List<Message> lowerCachedMessages = messageDao
                            .queryBuilder().limit((long) before + 1)
                            .orderBy("id", false).where().le("id", mainAnchor)
                            .and().ge("id", rng.low).query();
                    // however because of this we need to flip the ordering
                    Collections.reverse(lowerCachedMessages);
                    List<Message> upperCachedMessages = messageDao
                            .queryBuilder().limit((long) after)
                            .orderBy("id", true).where().gt("id", mainAnchor)
                            .and().le("id", rng.high).query();
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

                    if (lowerCachedMessages.size() > 0
                            || upperCachedMessages.size() > 0) {
                        if (before > 0) {
                            AsyncGetOldMessages beforeTask = new AsyncGetOldMessages(
                                    activity);
                            beforeTask.rng = rng;
                            beforeTask.execute(mainAnchor,
                                    HumbugActivity.LoadPosition.ABOVE, before,
                                    0);
                        }
                        if (after > 0) {

                            AsyncGetOldMessages afterTask = new AsyncGetOldMessages(
                                    activity);
                            afterTask.rng = rng;
                            afterTask
                                    .execute(afterAnchor,
                                            HumbugActivity.LoadPosition.BELOW,
                                            0, after);
                            Log.e("AGOM", "bck done");
                        }
                    }
                    return null;
                } else {
                    // We don't have anything cached
                    Log.e("db_gom", "No messages found in specified range!");
                }
            }
            if (fetchMessages(mainAnchor, before, after, params)) {
                if (rng != null) {
                    try {
                        rng.refresh();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Log.w("AGOM",
                                "Couldn't refresh rng, maybe not in database?");
                    }
                    if (before < rng.low) {
                        rng.low = before;
                    }
                    if (after > rng.high) {
                        rng.high = after;
                    }
                } else {
                    rng = new MessageRange(receivedMessages.get(0).getID(),
                            receivedMessages.get(receivedMessages.size() - 1)
                                    .getID());
                }

                // Consolidate ranges

                TransactionManager.callInTransaction(this.app
                        .getDatabaseHelper().getConnectionSource(),
                        new Callable<Void>() {
                            public Void call() throws Exception {
                                Where<MessageRange, Integer> where = messageRangeDao
                                        .queryBuilder().orderBy("low", true)
                                        .where();
                                @SuppressWarnings("unchecked")
                                List<MessageRange> ranges = where.or(
                                        where.and(where.ge("high", rng.low),
                                                where.le("high", rng.high)),
                                        where.and(where.ge("low", rng.low),
                                                where.le("low", rng.high)))
                                        .query();

                                if (ranges.size() == 0) {
                                    // Nothing to consolidate
                                    messageRangeDao.createOrUpdate(rng);
                                    return null;
                                }
                                Log.i("", "our low: " + rng.low
                                        + ", our high: " + rng.high);
                                int db_low = ranges.get(0).low;
                                int db_high = ranges.get(ranges.size() - 1).high;
                                Log.i("", "their low: " + db_low
                                        + ", their high: " + db_high);
                                if (db_low < rng.low) {
                                    rng.low = db_low;
                                }
                                if (db_high > rng.high) {
                                    rng.high = db_high;
                                }
                                messageRangeDao.delete(ranges);
                                messageRangeDao.createOrUpdate(rng);

                                return null;
                            }
                        });
            }
        } catch (SQLException e) {
            // Still welp.
            e.printStackTrace();
        }

        return null; // since onPostExecute doesn't use the String result
    }

    protected boolean fetchMessages(int anchor, int num_before, int num_after,
            String[] params) {

        this.setProperty("anchor", Integer.toString(anchor));
        this.setProperty("num_before", Integer.toString(num_before));
        this.setProperty("num_after", Integer.toString(num_after));
        this.setProperty("apply_markdown", "false");
        // We don't support narrowing at all, so always specify we're
        // unnarrowed.
        this.setProperty("narrow", "{}");

        String result = super.doInBackground(params);

        if (result != null) {
            try {
                JSONObject response = new JSONObject(result);
                JSONArray objects = response.getJSONArray("messages");

                Dao<Message, Integer> messages = this.app.getDatabaseHelper()
                        .getDao(Message.class);

                Message[] fetchedMessages = new Message[objects.length()];

                for (int i = 0; i < objects.length(); i++) {
                    Message message = new Message(this.app,
                            objects.getJSONObject(i));

                    fetchedMessages[i] = message;
                    messages.createOrUpdate(message);
                }
                if (num_after == 0) {
                    receivedMessages.addAll(0, Arrays.asList(fetchedMessages));
                } else {
                    receivedMessages.addAll(Arrays.asList(fetchedMessages));
                }
                return receivedMessages.size() > 0;
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("poll", "No data returned?");
                e.printStackTrace();
            } catch (SQLException e) {
                // Awkward. (TODO)
                e.printStackTrace();
            }
        } else {
            Log.i("poll", "got nothing from the server");
        }
        return false;
    }

    @Override
    protected void onPostExecute(String result) {
        if (receivedMessages != null && receivedMessages.size() != 0) {
            Log.v("poll", "Processing messages received.");
            activity.onMessages(receivedMessages.toArray(new Message[0]),
                    position);
        } else {
            Log.v("poll", "No messages returned.");
        }
        callback.onTaskComplete(result);
    }

    protected void onCancelled(String result) {
        callback.onTaskFailure(result);
    }
}
