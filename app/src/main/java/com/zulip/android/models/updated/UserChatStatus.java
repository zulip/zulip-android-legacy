package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;


public class UserChatStatus {

    @SerializedName("status")
    private String status;
    @SerializedName("timestamp")
    private int timestamp;
    @SerializedName("client")
    private String client;
    @SerializedName("pushable")
    private boolean pushable;

    public String getStatus() {
        return status;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getClient() {
        return client;
    }

    public boolean isPushable() {
        return pushable;
    }
}
