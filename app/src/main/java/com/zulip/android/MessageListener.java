package com.zulip.android;

public interface MessageListener {

    public enum LoadPosition {
        ABOVE, BELOW, NEW, INITIAL,
    }

    void onMessages(Message[] messages, LoadPosition pos, boolean moreAbove,
            boolean moreBelow, boolean noFurtherMessages);

    void onMessageError(LoadPosition pos);
}
