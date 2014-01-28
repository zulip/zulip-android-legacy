package com.zulip.android;

/**
 * Presence information for a user
 */
public class Presence {
    private final long age;
    private final String client;
    private final String status;

    public Presence(long age, String client, String status) {
        this.age = age;
        this.client = client;
        this.status = status;
    }

    public long getAge() {
        return age;
    }

    public String getClient() {
        return client;
    }

    public String getStatus() {
        return status;
    }
}
