/**
 * 
 */
package com.humbughq.android;

/**
 * @author lfaraone
 * 
 */
public enum MessageType {
    STREAM_MESSAGE("stream"), HUDDLE_MESSAGE("private"), PERSONAL_MESSAGE(
            "private");

    private String sendMessageType;

    MessageType(String sendMessageType) {
        this.sendMessageType = sendMessageType;
    }

    public String toString() {
        return this.sendMessageType;
    }
}
