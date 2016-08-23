package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class ZulipUser {

    @SerializedName("is_bot")
    private boolean isBot;
    @SerializedName("is_admin")
    private boolean isAdmin;
    @SerializedName("email")
    private String email;
    @SerializedName("full_name")
    private String fullName;

    public boolean isIsBot() {
        return isBot;
    }

    public void setIsBot(boolean isBot) {
        this.isBot = isBot;
    }

    public boolean isIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
