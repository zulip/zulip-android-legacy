package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class Referrals {
    @SerializedName("granted")
    private int granted;
    @SerializedName("used")
    private int used;

    public int getGranted() {
        return granted;
    }

    public void setGranted(int granted) {
        this.granted = granted;
    }

    public int getUsed() {
        return used;
    }

    public void setUsed(int used) {
        this.used = used;
    }
}
