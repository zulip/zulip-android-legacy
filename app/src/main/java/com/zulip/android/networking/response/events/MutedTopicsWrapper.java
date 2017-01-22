package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * TODO: add description
 * example : {"muted_topics":[["testing","(no topic)"]],"type":"muted_topics","id":42}
 */

public class MutedTopicsWrapper extends EventsBranch{
    @SerializedName("muted_topics")
    private List<List<String>> mutedTopics;

    public List<List<String>> getMutedTopics() {
        return this.mutedTopics;
    }

    public void setMutedTopics(List<List<String>> mutedTopics) {
        this.mutedTopics = mutedTopics;
    }
}
