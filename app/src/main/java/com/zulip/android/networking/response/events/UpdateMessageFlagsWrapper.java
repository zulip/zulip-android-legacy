package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * This class is used to deserialize the update_message_flags type events {@link EventsBranch.BranchType#UPDATE_MESSAGE_FLAGS}.
 * example: {"all":false,"timestamp":1488246444.8514339924,"messages":[6,7,9,10,14],"flag":"read","operation":"add","type":"update_message_flags","id":12}
 * {"all":false,"timestamp":1488247612.6961550713,"messages":[55],"flag":"starred","operation":"add","type":"update_message_flags","id":3}
 * {"all":false,"timestamp":1488247614.0365629196,"messages":[55],"flag":"starred","operation":"remove","type":"update_message_flags","id":4}
 */

public class UpdateMessageFlagsWrapper extends EventsBranch {
    @SerializedName("messages")
    private List<Integer> messageIds;

    @SerializedName("flag")
    private String flag;

    public String getFlag() {
        return this.flag;
    }

    public List<Integer> getMessageIds() {
        return this.messageIds;
    }
}
