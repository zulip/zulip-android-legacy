package com.zulip.android.util;

import android.content.SharedPreferences;

import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MutedTopics {

    private Set<String> mutedTopics;
    private static final String MUTED_TOPIC_KEY = "mutedTopics";

    public MutedTopics() {
        mutedTopics = new HashSet<>(getPrefs().getStringSet(MUTED_TOPIC_KEY, new HashSet<String>()));
    }

    public void addToMutedTopics(List<List<String>> mutedTopics) {
        Stream stream;

        if(mutedTopics != null) {
            for (int i = 0; i < mutedTopics.size(); i++) {
                List<String> mutedTopic = mutedTopics.get(i);
                stream = Stream.getByName(ZulipApp.get(), mutedTopic.get(0));
                this.mutedTopics.add(stream.getId() + mutedTopic.get(1));
            }
        }

        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putStringSet(MUTED_TOPIC_KEY, new HashSet<>(this.mutedTopics));
        editor.apply();
    }

    public boolean isTopicMute(int id, String subject) {
        return mutedTopics.contains(id + subject);
    }

    public boolean isTopicMute(Message message) {
        return mutedTopics.contains(message.concatStreamAndTopic());
    }

    public void muteTopic(Message message) {
        mutedTopics.add(message.concatStreamAndTopic());

        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putStringSet(MUTED_TOPIC_KEY, new HashSet<>(mutedTopics));
        editor.apply();
    }

    private SharedPreferences getPrefs() {
        return ZulipApp.get().getSettings();
    }


}
