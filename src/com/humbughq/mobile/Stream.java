package com.humbughq.mobile;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;

public class Stream {
    private String name;
    private int color;
    private Boolean inHomeView;
    private Boolean inviteOnly;

    /**
     * Construct a new Stream from JSON returned by the server.
     * 
     * @param message
     *            The JSON object parsed from the server's output
     * @throws JSONException
     *             Thrown if the JSON provided is malformed.
     */
    public Stream(JSONObject message) throws JSONException {
        name = message.getString("name");
        color = parseColor(message.getString("color"));
        inHomeView = message.getBoolean("in_home_view");
        inviteOnly = message.getBoolean("invite_only");
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public Boolean getInHomeView() {
        return inHomeView;
    }

    public Boolean getInviteOnly() {
        return inviteOnly;
    }

    public static int parseColor(String color) {
        // Color.parseColor does not handle colors of the form #f00.
        // Pre-process them into normal 6-char hex form.
        if (color.length() == 4) {
            char r = color.charAt(1);
            char g = color.charAt(2);
            char b = color.charAt(3);
            color = "#" + r + r + g + g + b + b;
        }
        return Color.parseColor(color);
    }
}
