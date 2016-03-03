package com.zulip.android;

public enum MessageType {
    STREAM_MESSAGE("stream"), PRIVATE_MESSAGE("private");

    private String jsonMessageType;

    MessageType(String sendMessageType) {
        this.jsonMessageType = sendMessageType;
    }

    public String toString() {
        return this.jsonMessageType;
    }
}
