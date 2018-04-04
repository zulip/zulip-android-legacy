package com.zulip.android.networking.response;

import com.google.gson.annotations.SerializedName;

public class StarResponse {
    @SerializedName("msg")
    private String message;

    @SerializedName("result")
    private String result;

    public String getMessage() {
        return message;
    }

    public String getResult() {
        return result;
    }
}
