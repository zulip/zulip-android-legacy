package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * This class is used to deserialize muted_topic type event
 * {@link EventsBranch.BranchType#MUTED_TOPICS}.
 *
 * example: {"muted_topics":[["devel","(no topic)"],["foraaron","(no topic)"],["announce","Streams"]],
 *      "type":"muted_topics","id":20}
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
