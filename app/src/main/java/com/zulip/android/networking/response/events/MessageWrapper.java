package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Message;

import java.util.List;


public class MessageWrapper extends EventsBranch {
    @SerializedName("message")
    private Message message;

    @SerializedName("flags")
    private List<?> flags;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public List<?> getFlags() {
        return flags;
    }

    public void setFlags(List<?> flags) {
        this.flags = flags;
    }


}
