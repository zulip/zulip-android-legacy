package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class ChatStatusWrapper {

    @SerializedName("website")
    private UserChatStatus website;

    public UserChatStatus getWebsite() {
        return website;
    }
}
