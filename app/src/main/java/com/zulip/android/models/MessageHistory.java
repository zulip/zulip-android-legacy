package com.zulip.android.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;


public class MessageHistory {
    @SerializedName("prev_content")
    public String prevContent;

    @SerializedName("prev_rendered_content")
    public String prevRenderedContent;

    @SerializedName("prev_rendered_content_version")
    public int prevRenderedContentVersion;

    @SerializedName("timestamp")
    public Date date;


}
