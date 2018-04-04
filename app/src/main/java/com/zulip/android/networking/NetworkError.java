package com.zulip.android.networking;

import com.google.gson.annotations.SerializedName;

public class NetworkError {

    @SerializedName("msg")
    private String msg;
    @SerializedName("result")
    private String result;

    public String getResult() {
        return result;
    }

    public String getMsg() {
        return msg;
    }
}
