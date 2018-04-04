package com.zulip.android.models;

/**
 * Type of presence activity
 */
public enum PresenceType {
    ACTIVE("active"), IDLE("idle");

    private String jsonMessageType;

    private PresenceType(String sendMessageType) {
        this.jsonMessageType = sendMessageType;
    }

    public String toString() {
        return this.jsonMessageType;
    }
}
