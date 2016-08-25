package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Person;

import java.util.List;

/**
 * Created by patrykpoborca on 8/26/16.
 */

public class MessageWrapper extends EventsBranch {
    @SerializedName("message")
    private ZulipMessage message;

    @SerializedName("flags")
    private List<?> flags;

    public ZulipMessage getMessage() {
        return message;
    }

    public void setMessage(ZulipMessage message) {
        this.message = message;
    }

    public List<?> getFlags() {
        return flags;
    }

    public void setFlags(List<?> flags) {
        this.flags = flags;
    }


    public static class ZulipMessage {

        @SerializedName("recipient_id")
        private int recipientId;

        @SerializedName("sender_email")
        private String senderEmail;

        @SerializedName("timestamp")
        private int timestamp;

        @SerializedName("sender_id")
        private int senderId;

        @SerializedName("sender_full_name")
        private String senderFullName;

        @SerializedName("sender_domain")
        private String senderDomain;

        @SerializedName("content")
        private String content;

        @SerializedName("gravatar_hash")
        private String gravatarHash;

        @SerializedName("avatar_url")
        private String avatarUrl;

        @SerializedName("client")
        private String client;

        @SerializedName("content_type")
        private String contentType;

        @SerializedName("sender_short_name")
        private String senderShortName;

        @SerializedName("type")
        private String type;

        @SerializedName("id")
        private int id;

        @SerializedName("subject")
        private String subject;

        @SerializedName("display_recipient")
        private List<Person> displayRecipient;

        @SerializedName("subject_links")
        private List<?> subjectLinks;
    }
}
