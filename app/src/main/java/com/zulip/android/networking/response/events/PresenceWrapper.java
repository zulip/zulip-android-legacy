package com.zulip.android.networking.response.events;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.models.Presence;

/**
 * This class is used to deserialize the presence type event {@link EventsBranch.BranchType#PRESENCE}
 *
 * example : {"id":0,"server_timestamp":1485171896.1950330734,"type":"presence","email":"example@gmail.com",
 *      "presence":{"ZulipAndroid":{"status":"active","timestamp":1485171896,"client":"ZulipAndroid","pushable":null}}}
 */
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
