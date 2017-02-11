package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Stream;

import java.util.List;

/**
 * This class is used to deserialize the subscription type events {@link EventsBranch.BranchType#SUBSCRIPTIONS}.
 *
 * example : Add operation  {"subscriptions":[{"desktop_notifications":true,"description":"For sales discussion",
 *      "color":"#f4ae55","name":"sales","stream_id":11,"subscribers":["emailgateway@zulip.com",
 *      "webhook-bot@zulip.com","welcome-bot@zulip.com","notification-bot@zulip.com",
 *      "new-user-bot@zulip.com","nagios-send-bot@zulip.com","nagios-receive-bot@zulip.com",
 *      "error-bot@zulip.com","default-bot@zulip.com","iago@zulip.com","prospero@zulip.com",
 *      "othello@zulip.com","AARON@zulip.com"],"invite_only":false,"audible_notifications":true,"email_address":"sales+51d742945283fef74dc08bdd4d251dde@localhost:9991",
 *      "pin_to_top":false,"in_home_view":true}],"type":"subscription","id":14,"op":"add"}
 *
 * UPDATE operation {"name":"sales","id":15,"property":"color","type":"subscription",
 *      "email":"AARON@zulip.com","value":"#c2c2c2","op":"update"}
 *
 * REMOVE operation {"subscriptions":[{"stream_id":7,"name":"design"}],"type":"subscription","id":16,"op":"remove"}
 *
 * {@link SubscriptionWrapper#operation} signifies the operation of the event
 * namely : add {@link SubscriptionWrapper#OPERATION_ADD},
 * update {@link SubscriptionWrapper#OPERATION_UPDATE} and
 * remove {@link SubscriptionWrapper#OPERATION_REMOVE}.
 *
 * {@link SubscriptionWrapper#property} holds the property updated and {@link SubscriptionWrapper#value}
 * holds the updated value of this property.
 */

public class SubscriptionWrapper<T> extends EventsBranch {

    public static final String OPERATION_ADD = "add";
    public static final String OPERATION_REMOVE = "remove";
    public static final String OPERATION_UPDATE = "update";

    @SerializedName("subscriptions")
    private List<T> streams;

    @SerializedName("op")
    private String operation;

    @SerializedName("name")
    private String streamName;

    @SerializedName("property")
    private String property;

    @SerializedName("email")
    private String email;

    @SerializedName("value")
    private Object value;

    public List<T> getStreams() {
        if (getOperation().equalsIgnoreCase(OPERATION_ADD) ||
                getOperation().equalsIgnoreCase(OPERATION_REMOVE) ||
                getOperation().equalsIgnoreCase(OPERATION_UPDATE)) {
            return this.streams;
        }
        return null;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * This function returns the updated stream object in case of an update subscription event.
     *
     * @param app {@link ZulipApp} instance
     * @return updated {@link Stream} object
     */
    public Stream getUpdatedStream(ZulipApp app) {
        if (this.operation.equalsIgnoreCase(SubscriptionWrapper.OPERATION_UPDATE)) {
            // TODO: account for other updates
            if (property.equalsIgnoreCase("color")) {
                // color of stream is changed
                Stream stream = Stream.getByName(app, streamName);
                stream.setFetchColor((String) this.value);
                return stream;
            } else if (property.equalsIgnoreCase("in_home_view")) {
                // stream is muted or unmuted
                Stream stream = Stream.getByName(app, streamName);
                stream.setInHomeView((boolean) this.value);
                return stream;
            }
        }

        return null;
    }
}
