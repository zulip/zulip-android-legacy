package com.zulip.android.models;

import com.google.gson.annotations.SerializedName;



public class ZulipClientPresence {
    @SerializedName("status")
    private String status;

    @SerializedName("timestamp")
    private int timestamp;

    @SerializedName("client")
    private String client;

    @SerializedName("pushable")
    private Object pushable;

    public String getStatus() {
        return status;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getClient() {
        return client;
    }

    public Object getPushable() {
        return pushable;
    }
}
