package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;



public class ChatStatusWrapper {

    @SerializedName("website")
    private UserChatStatus website;

    public UserChatStatus getWebsite() {
        return website;
    }
}
