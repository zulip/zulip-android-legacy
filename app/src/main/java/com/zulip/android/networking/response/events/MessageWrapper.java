package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Message;

import java.util.List;

/**
 * This class is used to deserialize the message type event {@link EventsBranch.BranchType#MESSAGE}
 *
 * example : {"flags":[],"message":{"reactions":[],"recipient_id":38,
 *      "sender_email":"error-bot@zulip.com","timestamp":1485172576,"display_recipient":"logs",
 *      "sender_id":10,"sender_full_name":"Zulip Error Bot","sender_domain":"zulip.com",
 *      "content":"<div class=\"codehilite\"><pre><span></span>10.0.3.1        POST    200  4.1s (db: 217ms/2q) /api/v1/users/me/presence (AARON@zulip.com via ZulipAndroid) (AARON@zulip.com)\n</pre></div>",
 *      "stream_id":15,"gravatar_hash":"de425733f46e97dae34e4282037edd7e",
 *      "avatar_url":"https://secure.gravatar.com/avatar/de425733f46e97dae34e4282037edd7e?d=identicon",
 *      "client":"Internal","content_type":"text/html","subject_links":[],"is_mentioned":false,
 *      "sender_short_name":"error-bot","type":"stream","id":1522,"subject":"localhost:9991: slow queries"},
 *      "type":"message","id":8}
 */
public class MessageWrapper extends EventsBranch {
    @SerializedName("message")
    private Message message;

    @SerializedName("flags")
    private List<?> flags;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public List<?> getFlags() {
        return flags;
    }

    public void setFlags(List<?> flags) {
        this.flags = flags;
    }


}
