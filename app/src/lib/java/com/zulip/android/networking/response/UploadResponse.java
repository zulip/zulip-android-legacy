package com.zulip.android.networking.response;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    @SerializedName("uri")
    private String mUri;

    public String getUri() {
        return mUri;
    }
}
