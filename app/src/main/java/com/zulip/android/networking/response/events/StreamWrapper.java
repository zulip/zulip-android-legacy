package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Stream;

import java.util.List;

/**
 * This class is used to deserialize the stream type events {@link EventsBranch.BranchType#STREAM}.
 * example: {"streams":[{"stream_id":20,"description":"","name":"Good Morning","invite_only":false}],
 * "type":"stream","id":22,"op":"occupy"}
 */

public class StreamWrapper extends EventsBranch {

    public static final String OP_OCCUPY = "occupy";

    @SerializedName("streams")
    private List<Stream> mStreams;

    @SerializedName("op")
    private String mOperation;

    public List<Stream> getStreams() {
        return this.mStreams;
    }

    public String getOperation() {
        return this.mOperation;
    }
}
