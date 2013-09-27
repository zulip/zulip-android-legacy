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

import com.humbughq.mobile.MessageListener.LoadPosition;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.Where;

public class AsyncGetOldMessages extends HumbugAsyncPushTask {
    MessageListFragment fragment;
    public ArrayList<Message> receivedMessages;
    MessageListener.LoadPosition position;
    protected MessageRange rng;
    protected int mainAnchor;
    protected NarrowFilter filter;
    private int before;
    private int afterAnchor;
    private int after;
    AsyncGetOldMessages that = this;

    public AsyncGetOldMessages(MessageListFragment fragment) {
        super(fragment.app);
        this.fragment = fragment;
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
        execute("GET", "v1/messages");
    }

    @Override
    protected String doInBackground(String... params) {
        // Lets see whether we have this cached already
        final Dao<MessageRange, Integer> messageRangeDao = app
                .getDao(MessageRange.class);
        Dao<Message, Object> messageDao = app.getDao(Message.class);
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

                    if (lowerCachedMessages.size() > 0
                            || upperCachedMessages.size() > 0) {
                        if (before > 0) {
                            this.recurse(LoadPosition.ABOVE, before, rng,
                                    mainAnchor);
                        }
                        if (after > 0) {
                            this.recurse(LoadPosition.BELOW, after, rng,
                                    afterAnchor);
                        }
                    }
                    return null;
                } else {
                    // We don't have anything cached
                    Log.e("db_gom", "No messages found in specified range!");
                }
            }
            if (fetchMessages(mainAnchor, before, after, params)) {
                int lowest = receivedMessages.get(0).getID();
                int highest = receivedMessages.get(receivedMessages.size() - 1)
                        .getID();
                if (rng != null) {
                    try {
                        rng.refresh();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Log.w("AGOM",
                                "Couldn't refresh rng, maybe not in database?");
                    }
                    if (lowest < rng.low) {
                        rng.low = lowest;
                    }
                    if (highest > rng.high) {
                        rng.high = highest;
                    }
                } else {
                    rng = new MessageRange(lowest, highest);
                }

                // Consolidate ranges, except in a narrow

                if (filter == null) {
                    TransactionManager.callInTransaction(this.app
                            .getDatabaseHelper().getConnectionSource(),
                            new Callable<Void>() {
                                public Void call() throws Exception {
                                    Where<MessageRange, Integer> where = messageRangeDao
                                            .queryBuilder()
                                            .orderBy("low", true).where();
                                    @SuppressWarnings("unchecked")
                                    List<MessageRange> ranges = where
                                            .or(where.and(
                                                    where.ge("high", rng.low),
                                                    where.le("high", rng.high)),
                                                    where.and(where.ge("low",
                                                            rng.low), where.le(
                                                            "low", rng.high)))
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
            }
        } catch (SQLException e) {
            // Still welp.
            e.printStackTrace();
        }

        return null; // since onPostExecute doesn't use the String result
    }

    protected void recurse(LoadPosition position, int amount, MessageRange rng,
            int anchor) {
        AsyncGetOldMessages task = new AsyncGetOldMessages(fragment);
        task.rng = rng;
        switch (position) {
        case ABOVE:
            task.execute(anchor, position, amount, 0, filter);
            break;
        case BELOW:
            task.execute(anchor, position, 0, amount, filter);
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
        this.setProperty("apply_markdown", "false");
        // We don't support narrowing at all, so always specify we're
        // unnarrowed.
        if (filter != null) {
            try {
                this.setProperty("narrow", filter.getJsonFilter());
            } catch (JSONException e) {
                Log.wtf("fm", e);
            }
        } else {
            this.setProperty("narrow", "{}");
        }

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

            fragment.onMessages(receivedMessages.toArray(new Message[0]),
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
