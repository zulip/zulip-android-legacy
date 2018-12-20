package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;


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
