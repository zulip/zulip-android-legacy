package com.zulip.android.networking.response;

import com.google.gson.annotations.SerializedName;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class LoginResponse {

    @SerializedName("msg")
    private String msg;

    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("result")
    private String result;

    @SerializedName("email")
    private String email;

    public String getMsg() {
        return msg;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getResult() {
        return result;
    }

    public String getEmail() {
        return email;
    }
}
