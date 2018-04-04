package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;

public class EditMessageWrapper extends EventsBranch {
    @SerializedName("data")
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public class Data {
        @SerializedName("allow_message_editing")
        private boolean isMessageEditingAllowed;

        @SerializedName("message_content_edit_limit_seconds")
        private int messageContentEditLimitSeconds;

        public boolean isMessageEditingAllowed() {
            return isMessageEditingAllowed;
        }

        public int getMessageContentEditLimitSeconds() {
            return messageContentEditLimitSeconds;
        }
    }
}
