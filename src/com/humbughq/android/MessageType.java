/**
 * 
 */
package com.humbughq.android;

/**
 * @author lfaraone
 * 
 */
public enum MessageType {
    STREAM_MESSAGE("stream"), HUDDLE_MESSAGE("personal"), PERSONAL_MESSAGE(
            "personal");

    private String sendMessageType;

    MessageType(String sendMessageType) {
        this.sendMessageType = sendMessageType;
    }

    public String toString() {
        return this.sendMessageType;
    }
}
