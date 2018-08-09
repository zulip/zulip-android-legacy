package com.zulip.android.models.updated;


import com.google.gson.annotations.SerializedName;

public class JenkinsType {

    @SerializedName("sourceUrl")
    private String sourceUrl;

    @SerializedName("displayUrl")
    private String displayUrl;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }
}
