package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Stream;

import java.util.List;

/**
 * TODO: add description
 */

public class SubscriptionWrapper extends EventsBranch {

    public static final String OPERATION_ADD = "add";
    public static final String OPERATION_REMOVE = "remove";
    public static final String OPERATION_UPDATE = "update";

    @SerializedName("subscriptions")
    private List<Stream> streams;

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

    public List<Stream> getStreams() {
        return this.streams;
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Stream getUpdatedStream() {
        if (this.operation.equalsIgnoreCase(SubscriptionWrapper.OPERATION_UPDATE)) {
            // TODO: account for other updates as well
            if (property.equalsIgnoreCase("color")) {
                Stream stream = Stream.getByName(ZulipApp.get(), streamName);
                stream.setFetchColor((String) this.value);
                return stream;
            } else if (property.equalsIgnoreCase("in_home_view")) {
                // stream mute unmute
                Stream stream = Stream.getByName(ZulipApp.get(), streamName);
                stream.setInHomeView((boolean) this.value);
                return stream;
            }
        }

        return null;
    }
}
