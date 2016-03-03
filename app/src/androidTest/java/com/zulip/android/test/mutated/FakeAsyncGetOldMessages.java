package com.zulip.android.test.mutated;

import java.util.ArrayList;
import java.util.List;

import com.zulip.android.Message;
import com.zulip.android.MessageListFragment;
import com.zulip.android.MessageRange;
import com.zulip.android.MessageListener.LoadPosition;

public class FakeAsyncGetOldMessages extends
        com.zulip.android.AsyncGetOldMessages {
    public String calculatedResult;
    public int fmAnchor;
    public int fmNumBefore;
    public int fmNumAfter;
    public boolean shouldFmSucceed;
    public boolean fmCalled;
    public List<Message> appendTheseMessages;
    public List<FakeAsyncGetOldMessages> recurseRequestsReceived;
    private MessageListFragment myfragment;

    public FakeAsyncGetOldMessages(MessageListFragment fragment) {
        super(fragment);
        this.app = fragment.app;
        myfragment = fragment;
        fmCalled = false;
        shouldFmSucceed = false;
        recurseRequestsReceived = new ArrayList<FakeAsyncGetOldMessages>();
        filter = null;
    }

    public void executeBasedOnPresetValues() {
        // LP doesn't matter, so just go with INITIAL
        this.execute(fmAnchor, LoadPosition.INITIAL, fmNumBefore, fmNumAfter,
                filter);
    }

    @Override
    protected boolean fetchMessages(int anchor, int num_before, int num_after,
            String[] params) {
        fmCalled = true;
        fmAnchor = anchor;
        fmNumBefore = num_before;
        fmNumAfter = num_after;
        if (appendTheseMessages != null) {
            this.receivedMessages.addAll(appendTheseMessages);
        }
        return shouldFmSucceed;
    }

    @Override
    protected void recurse(LoadPosition position, int amount, MessageRange rng,
            int anchor) {
        FakeAsyncGetOldMessages task = new FakeAsyncGetOldMessages(myfragment);
        task.rng = rng;
        if (position == LoadPosition.ABOVE) {
            task.fmNumBefore = amount;
        } else {
            task.fmNumAfter = amount;
        }
        task.fmAnchor = anchor;
        recurseRequestsReceived.add(task);
    }

    @Override
    protected void onPostExecute(String result) {
        calculatedResult = result;
    }

}
