package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Reaction;
import com.zulip.android.models.UserReaction;

/**
 * This class is used to deserialize the reaction type event {@link EventsBranch.BranchType#REACTION}
 * example: {"emoji_name":"alarm_clock","id":54,"user":{"user_id":346,"email":"abc@gmail.com","full_name":"anonymous user"},"type":"reaction","message_id":163213,"op":"add"}
 * {"emoji_name":"alarm_clock","id":72,"user":{"user_id":346,"email":"abc@gmail.com","full_name":"anonymous user"},"type":"reaction","message_id":163213,"op":"remove"}
 */

public class ReactionWrapper extends EventsBranch {
    public static final String OPERATION_ADD = "add";
    public static final String OPERATION_REMOVE = "remove";

    @SerializedName("emoji_name")
    private String emoji;

    @SerializedName("user")
    private UserReaction user;

    @SerializedName("message_id")
    private int messageId;

    @SerializedName("op")
    private String operation;

    public ReactionWrapper() {
    }

    public int getMessageId() {
        return this.messageId;
    }

    public Reaction getReaction() {
        // correctly initialize User object
        if (this.user != null) {
            this.user.setId(this.user.getAlternateId());
        }

        Reaction retVal = new Reaction();
        retVal.setEmoji(this.emoji);
        retVal.setUser(this.user);
        return retVal;
    }

    public String getOperation() {
        return this.operation;
    }
}
