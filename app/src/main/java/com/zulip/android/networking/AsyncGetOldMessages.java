package com.zulip.android.networking;

import android.util.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilter;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageRange;
import com.zulip.android.networking.response.GetMessagesResponse;
import com.zulip.android.util.MessageListener;
import com.zulip.android.util.MessageListener.LoadPosition;

import org.apache.commons.lang.time.StopWatch;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

public class AsyncGetOldMessages extends ZulipAsyncPushTask {
    private MessageListener listener;
    public List<Message> receivedMessages;
    private MessageListener.LoadPosition position;
    protected MessageRange rng;
    private int mainAnchor;
    protected NarrowFilter filter;
    private int before;
    private int afterAnchor;
    private int after;

    private boolean recursedAbove = false;
    private boolean recursedBelow = false;

    private boolean noFurtherMessages = false;
    private int rangeHigh = -2;

    public AsyncGetOldMessages(MessageListener listener) {
        super(ZulipApp.get());
        this.listener = listener;
        rng = null;
    }

    /**
     * Get messages surrounding a specified anchor message ID, inclusive of both
     * endpoints and the anchor.
     *
     * @param anchor Message ID of the message to fetch around
     * @param before Number of messages after the anchor to return
     * @param after  Number of messages before the anchor to return
     */
    public final void execute(int anchor, LoadPosition pos, int before,
                              int after, NarrowFilter filter) {
        this.mainAnchor = anchor;
        this.before = before;
        this.afterAnchor = mainAnchor + 1;
        this.after = after;
        this.filter = filter;
        position = pos;
        this.receivedMessages = new ArrayList<>();
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
                    if (!lowerCachedMessages.isEmpty()) {
                        mainAnchor = lowerCachedMessages.get(0).getID() - 1;
                    }
                    if (!upperCachedMessages.isEmpty()) {
                        afterAnchor = upperCachedMessages.get(
                                upperCachedMessages.size() - 1).getID() + 1;
                    }
                    receivedMessages.addAll(lowerCachedMessages);
                    receivedMessages.addAll(upperCachedMessages);

                    watch.stop();
                    Log.i("perf",
                            "Retrieving cached messages: " + watch.toString());

                    if (!lowerCachedMessages.isEmpty()
                            || !upperCachedMessages.isEmpty()) {
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
            if (fetchMessages(mainAnchor, before, after, params) && filter == null) {
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

    protected boolean fetchMessages(int anchor, int numBefore, int numAfter,
                                    String[] params) {

        Response<GetMessagesResponse> result;
        try{
             result = app.getZulipServices()
                    .getMessages(
                            Integer.toString(anchor),
                            Integer.toString(numBefore),
                            Integer.toString(numAfter),
                            filter)
                    .execute();
            if(!result.isSuccessful()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        List<Message> fetchedMessages = result.body().getMessages();
        Message.createMessages(app, fetchedMessages);

        if (numAfter == 0) {
            receivedMessages.addAll(0, fetchedMessages);
        } else {
            receivedMessages.addAll(fetchedMessages);
        }

        if ((position == LoadPosition.ABOVE || position == LoadPosition.BELOW)
                && receivedMessages.size() < numBefore + numAfter) {
            noFurtherMessages = true;
        }

        return !receivedMessages.isEmpty();
    }

    @Override
    protected void onPostExecute(String result) {
        if (receivedMessages != null) {
            Log.v("poll", "Processing messages received.");

            if ((position == LoadPosition.BELOW || position == LoadPosition.INITIAL)
                    && app.getMaxMessageId() == rangeHigh) {
                noFurtherMessages = true;
            }

            listener.onMessages(receivedMessages.toArray(new Message[receivedMessages.size()]),
                    position, recursedAbove, recursedBelow, noFurtherMessages);
            callback.onTaskComplete(receivedMessages.size() + "", null);
        } else {
            listener.onMessageError(position);
            Log.v("poll", "No messages returned.");
            callback.onTaskFailure("");
        }
    }

    protected void onCancelled(String result) {
        listener.onMessageError(position);
        callback.onTaskFailure(result);
    }
}
