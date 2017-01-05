package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Presence;


public class PresenceWrapper extends EventsBranch {

    @SerializedName("server_timestamp")
    private double serverTimestamp;

    @SerializedName("email")
    private String email;

    @SerializedName("presence")
    private Presence presence;

    public Presence getPresence() {
        return presence;
    }

    public double getServerTimestamp() {
        return serverTimestamp;
    }

    public String getEmail() {
        return email;
    }
}
