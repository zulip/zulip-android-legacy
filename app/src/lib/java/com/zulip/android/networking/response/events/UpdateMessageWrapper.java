package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.Dao;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.util.ZLog;

import java.sql.SQLException;
import java.util.List;

/**
 * This class is used to deserialize the update_message type events {@link EventsBranch.BranchType#UPDATE_MESSAGE}.
 * <p>
 * example: {"rendered_content":"<p>(deleted</p>","sender":"example@gmail.com",
 * "edit_timestamp":1485954038,"orig_content":"(deleted)","message_ids":[142650],
 * "content":"(deleted","orig_rendered_content":"<p>(deleted)</p>","flags":["read"],"id":31,
 * "type":"update_message","message_id":142650}
 */

public class UpdateMessageWrapper extends EventsBranch {
    @SerializedName("rendered_content")
    private String formattedContent;

    @SerializedName("sender")
    private String senderEmail;

    @SerializedName("edit_timestamp")
    private long editedTimeStamp;

    @SerializedName("orig_content")
    private String origContent;

    @SerializedName("message_ids")
    private List<Integer> messageIds;

    @SerializedName("content")
    private String content;

    @SerializedName("orig_rendered_content")
    private String origFormattedContent;

    @SerializedName("flags")
    private List<?> flags;

    @SerializedName("message_id")
    private int messageId;

    @SerializedName("subject")
    private String subject;

    /**
     * Returns old form of edited message.
     *
     * @return {@link Message} message
     */
    public Message getMessage(int messageId) {
        try {
            Dao<Message, Integer> messageDao = ZulipApp.get().getDao(Message.class);
            Message message = messageDao.queryBuilder().where().eq(Message.ID_FIELD, messageId).queryForFirst();
            if (message != null) {
                if (this.formattedContent != null) {
                    message.setFormattedContent(this.formattedContent);
                    message.setHasBeenEdited(true);
                }
                if (containsSubject()) {
                    message.setSubject(this.subject);
                }
                return message;
            }
        } catch (SQLException e) {
            ZLog.logException(e);
        }

        return null;
    }

    public List<Integer> getMessageIds() {
        return this.messageIds;
    }

    public boolean containsSubject() {
        return this.subject != null;
    }
}
