package com.zulip.android.models.updated;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by patrykpoborca on 8/25/16.
 */

public class ZulipStream {


    /**
     * description : Automated issue traffic on Android repository
     * color : #a6dcbf
     * subscribers : [23,7,150,22]
     * audible_notifications : false
     * email_address :
     * desktop_notifications : true
     * name : android-issues
     * stream_id : 29
     * invite_only : false
     * pin_to_top : false
     * in_home_view : true
     */

    @SerializedName("description")
    private String description;
    @SerializedName("color")
    private String color;
    @SerializedName("audible_notifications")
    private boolean audibleNotifications;
    @SerializedName("email_address")
    private String emailAddress;
    @SerializedName("desktop_notifications")
    private boolean desktopNotifications;
    @SerializedName("name")
    private String name;
    @SerializedName("stream_id")
    private int streamId;
    @SerializedName("invite_only")
    private boolean inviteOnly;
    @SerializedName("pin_to_top")
    private boolean pinToTop;
    @SerializedName("in_home_view")
    private boolean inHomeView;
    @SerializedName("subscribers")
    private List<Integer> subscribers;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isAudibleNotifications() {
        return audibleNotifications;
    }

    public void setAudibleNotifications(boolean audibleNotifications) {
        this.audibleNotifications = audibleNotifications;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean isDesktopNotifications() {
        return desktopNotifications;
    }

    public void setDesktopNotifications(boolean desktopNotifications) {
        this.desktopNotifications = desktopNotifications;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public boolean isInviteOnly() {
        return inviteOnly;
    }

    public void setInviteOnly(boolean inviteOnly) {
        this.inviteOnly = inviteOnly;
    }

    public boolean isPinToTop() {
        return pinToTop;
    }

    public void setPinToTop(boolean pinToTop) {
        this.pinToTop = pinToTop;
    }

    public boolean isInHomeView() {
        return inHomeView;
    }

    public void setInHomeView(boolean inHomeView) {
        this.inHomeView = inHomeView;
    }

    public List<Integer> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<Integer> subscribers) {
        this.subscribers = subscribers;
    }
}
