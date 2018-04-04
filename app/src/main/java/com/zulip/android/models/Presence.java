package com.zulip.android.models;

import com.google.gson.annotations.SerializedName;

/**
 * Presence information for a user
 */
public class Presence {
    private final long age;
    private final String client;
    private final PresenceType status;

    @SerializedName("ZulipAndroid")
    private ZulipClientPresence presence;

    public Presence(long age, String client, PresenceType status) {
        this.age = age;
        this.client = client;
        this.status = status;
    }

    public ZulipClientPresence getPresence() {
        return presence;
    }

    public long getAge() {
        return age;
    }

    public String getClient() {
        return client;
    }

    public PresenceType getStatus() {
        return status;
    }
}
