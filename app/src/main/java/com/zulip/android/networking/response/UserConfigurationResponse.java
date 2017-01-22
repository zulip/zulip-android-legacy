package com.zulip.android.networking.response;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;
import com.zulip.android.models.updated.ChatStatusWrapper;
import com.zulip.android.models.updated.JenkinsType;
import com.zulip.android.models.updated.Referrals;

import java.util.List;
import java.util.Map;


public class UserConfigurationResponse {

    @SerializedName("last_event_id")
    private int lastEventId;

    @SerializedName("realm_domain")
    private String realmDomain;

    @SerializedName("realm_name")
    private String realmName;

    @SerializedName("result")
    private String result;

    @SerializedName("realm_default_language")
    private String realmDefaultLanguage;

    @SerializedName("presences")
    private Map<String, ChatStatusWrapper> presences;

    @SerializedName("pointer")
    private int pointer;

    @SerializedName("realm_create_stream_by_admins_only")
    private boolean realmCreateStreamByAdminsOnly;

    @SerializedName("email_dict")
    private Map<String, String> emailDict;

    @SerializedName("msg")
    private String msg;

    @SerializedName("realm_allow_message_editing")
    private boolean realmAllowMessageEditing;

    @SerializedName("realm_emoji")
    private Map<String, JenkinsType> realmEmoji;

    @SerializedName("default_language")
    private String defaultLanguage;

    @SerializedName("realm_restricted_to_domain")
    private boolean realmRestrictedToDomain;

    @SerializedName("max_message_id")
    private int maxMessageId;

    @SerializedName("queue_id")
    private String queueId;

    @SerializedName("realm_message_content_edit_limit_seconds")
    private int realmMessageContentEditLimitSeconds;

    @SerializedName("realm_invite_by_admins_only")
    private boolean realmInviteByAdminsOnly;

    @SerializedName("realm_invite_required")
    private boolean realmInviteRequired;

    @SerializedName("referrals")
    private Referrals referrals;

    @SerializedName("twenty_four_hour_time")
    private boolean twentyFourHourTime;

    @SerializedName("left_side_userlist")
    private boolean leftSideUserlist;

    @SerializedName("muted_topics")
    private List<List<String>> mutedTopics;

    //todo unkown type
    @SerializedName("alert_words")
    private List<?> alertWords;

    //todo unknown type
    @SerializedName("realm_bots")
    private List<?> realmBots;

    @SerializedName("never_subscribed")
    private List<Stream> neverSubscribed;

    @SerializedName("realm_default_streams")
    private List<Stream> realmDefaultStreams;

    @SerializedName("unsubscribed")
    private List<Stream> unsubscribed;

    @SerializedName("subscriptions")
    private List<Stream> subscriptions;

    @SerializedName("streams")
    private List<Stream> streams;

    @SerializedName("realm_filters")
    private List<List<String>> realmFilters;

    @SerializedName("realm_users")
    private List<Person> realmUsers;


    public int getLastEventId() {
        return lastEventId;
    }

    public String getRealmDomain() {
        return realmDomain;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getResult() {
        return result;
    }

    public String getRealmDefaultLanguage() {
        return realmDefaultLanguage;
    }

    public Map<String, ChatStatusWrapper> getPresences() {
        return presences;
    }

    public int getPointer() {
        return pointer;
    }

    public boolean isRealmCreateStreamByAdminsOnly() {
        return realmCreateStreamByAdminsOnly;
    }

    public Map<String, String> getEmailDict() {
        return emailDict;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isRealmAllowMessageEditing() {
        return realmAllowMessageEditing;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isRealmRestrictedToDomain() {
        return realmRestrictedToDomain;
    }

    public int getMaxMessageId() {
        return maxMessageId;
    }

    public String getQueueId() {
        return queueId;
    }

    public int getRealmMessageContentEditLimitSeconds() {
        return realmMessageContentEditLimitSeconds;
    }

    public boolean isRealmInviteByAdminsOnly() {
        return realmInviteByAdminsOnly;
    }

    public boolean isRealmInviteRequired() {
        return realmInviteRequired;
    }

    public Referrals getReferrals() {
        return referrals;
    }

    public boolean isTwentyFourHourTime() {
        return twentyFourHourTime;
    }

    public boolean isLeftSideUserlist() {
        return leftSideUserlist;
    }

    public List<List<String>> getMutedTopics() {
        return mutedTopics;
    }

    public List<?> getAlertWords() {
        return alertWords;
    }

    public List<?> getRealmBots() {
        return realmBots;
    }

    public List<Stream> getNeverSubscribed() {
        return neverSubscribed;
    }

    public List<Stream> getRealmDefaultStreams() {
        return realmDefaultStreams;
    }

    public List<Stream> getUnsubscribed() {
        return unsubscribed;
    }

    public List<Stream> getSubscriptions() {
        return subscriptions;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public List<List<String>> getRealmFilters() {
        return realmFilters;
    }

    public List<Person> getRealmUsers() {
        return realmUsers;
    }

    public JenkinsType getRealmEmoji() {
        return realmEmoji == null ? null : realmEmoji.get("jenkins");
    }
}
