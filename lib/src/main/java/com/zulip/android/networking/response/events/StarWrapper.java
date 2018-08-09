package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StarWrapper extends EventsBranch {
    @SerializedName("messages")
    private List<Integer> messageContent;

    @SerializedName("flag")
    private String flag;

    @SerializedName("operation")
    private String operation;

    public List<Integer> getMessageId() {
        return messageContent;
    }

    public String getFlag() {
        return flag;
    }

    public String getOperation() {
        return operation;
    }
}
