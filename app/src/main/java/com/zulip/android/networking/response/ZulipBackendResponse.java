package com.zulip.android.networking.response;

import com.google.gson.annotations.SerializedName;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class ZulipBackendResponse {

    @SerializedName("msg")
    private String msg;

    @SerializedName("password")
    private boolean password;

    @SerializedName("google")
    private boolean google;

    @SerializedName("result")
    private String result;

    @SerializedName("dev")
    private boolean dev;

    public String getMsg() {
        return msg;
    }

    public boolean isPassword() {
        return password;
    }

    public boolean isGoogle() {
        return google;
    }

    public String getResult() {
        return result;
    }

    public boolean isDev() {
        return dev;
    }
}
