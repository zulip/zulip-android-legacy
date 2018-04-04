package com.zulip.android.networking.response;


import com.google.gson.annotations.SerializedName;

/**
 * Used for fetching the raw content of any message (in markdown format)
 * <p>
 * example : {"msg":"","result":"success","raw_content":":+1: texthere\n"}
 */
public class RawMessageResponse {

    @SerializedName("msg")
    private String msg;

    @SerializedName("result")
    private String result;

    @SerializedName("raw_content")
    private String rawContent;

    public String getMsg() {
        return msg;
    }

    public String getResult() {
        return result;
    }

    public String getRawContent() {
        return rawContent;
    }
}
