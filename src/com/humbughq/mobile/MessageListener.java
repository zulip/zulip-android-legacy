package com.humbughq.mobile;

public interface MessageListener {

    public enum LoadPosition {
        ABOVE, BELOW, NEW, INITIAL,
    }

    void onMessages(Message[] messages, LoadPosition pos, boolean moreAbove,
            boolean moreBelow);

    void onMessageError(LoadPosition pos);
}
