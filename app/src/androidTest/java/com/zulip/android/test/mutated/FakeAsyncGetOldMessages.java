package com.zulip.android.test.mutated;

import com.zulip.android.activities.MessageListFragment;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageRange;
import com.zulip.android.networking.AsyncGetOldMessages;
import com.zulip.android.util.MessageListener.LoadPosition;

import java.util.ArrayList;
import java.util.List;

public class FakeAsyncGetOldMessages extends
        AsyncGetOldMessages {
    public List<Message> appendTheseMessages;
    public List<FakeAsyncGetOldMessages> recurseRequestsReceived;
    private String calculatedResult;
    private int fmAnchor;
    private int fmNumBefore;
    private int fmNumAfter;
    private boolean shouldFmSucceed;
    private boolean fmCalled;
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
    protected boolean fetchMessages(int anchor, int numBefore, int numAfter,
                                    String[] params) {
        fmCalled = true;
        fmAnchor = anchor;
        fmNumBefore = numBefore;
        fmNumAfter = numAfter;
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

    public String getCalculatedResult() {
        return calculatedResult;
    }

    public void setCalculatedResult(String calculatedResult) {
        this.calculatedResult = calculatedResult;
    }

    public int getFmAnchor() {
        return fmAnchor;
    }

    public void setFmAnchor(int fmAnchor) {
        this.fmAnchor = fmAnchor;
    }

    public int getFmNumBefore() {
        return fmNumBefore;
    }

    public void setFmNumBefore(int fmNumBefore) {
        this.fmNumBefore = fmNumBefore;
    }

    public int getFmNumAfter() {
        return fmNumAfter;
    }

    public void setFmNumAfter(int fmNumAfter) {
        this.fmNumAfter = fmNumAfter;
    }

    public boolean shouldFmSucceed() {
        return shouldFmSucceed;
    }

    public void setShouldFmSucceed(boolean shouldFmSucceed) {
        this.shouldFmSucceed = shouldFmSucceed;
    }

    public boolean isFmCalled() {
        return fmCalled;
    }

    public void setFmCalled(boolean fmCalled) {
        this.fmCalled = fmCalled;
    }
}
