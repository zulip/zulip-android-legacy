package com.zulip.android.models;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.table.DatabaseTable;
import com.zulip.android.ZulipApp;
import com.zulip.android.util.ZLog;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.sql.SQLException;
import java.util.List;

@DatabaseTable(tableName = "streams")
public class Stream {
    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String COLOR_FIELD = "color";
    public static final String SUBSCRIBED_FIELD = "subscribed";
    private static final int DEFAULT_COLOR = Color.GRAY;
    private static final String MESSAGES_FIELD = "messages";
    private static final String INHOMEVIEW_FIELD = "inHomeView";
    private static final String INVITEONLY_FIELD = "inviteOnly";
    @SerializedName("stream_id")
    @DatabaseField(columnName = ID_FIELD, id = true)
    private int id;

    @SerializedName("description")
    private String description;

    @SerializedName("subscribers")
    private List<String> subscribers;

    @SerializedName("pin_to_top")
    private boolean pinToTop;

    @SerializedName("audible_notifications")
    private boolean audibleNotifications;

    @SerializedName("email_address")
    private String emailAddress;

    @SerializedName("desktop_notifications")
    private boolean desktopNotifications;

    @ForeignCollectionField(columnName = MESSAGES_FIELD)
    private ForeignCollection<Message> messages;

    @DatabaseField(columnName = SUBSCRIBED_FIELD)
    private boolean subscribed;

    @DatabaseField(columnName = NAME_FIELD, uniqueIndex = true)
    @SerializedName("name")
    private String name;

    @DatabaseField(columnName = COLOR_FIELD)
    private int parsedColor;

    @SerializedName("color")
    private String fetchedColor;

    @DatabaseField(columnName = INVITEONLY_FIELD)
    @SerializedName("invite_only")
    private boolean inviteOnly;

    @DatabaseField(columnName = INHOMEVIEW_FIELD)
    @SerializedName("in_home_view")
    private boolean inHomeView;

    /**
     * Construct an empty Stream object.
     */
    public Stream() {
        this.subscribed = false;
    }

    /**
     * Construct a new Stream object when all that's known is the name.
     * <p/>
     * These should be sensible defaults.
     *
     * @param name The stream name
     */
    public Stream(String name) {
        this.name = name;
        parsedColor = DEFAULT_COLOR;
        inHomeView = true; // Sure, why not
        inviteOnly = false; // Most probably
    }

    private static int parseColor(String color) {
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

    public static Stream getByName(ZulipApp app, String name) {
        Stream stream = null;
        try {
            RuntimeExceptionDao<Stream, Object> streams = app.getDao(Stream.class);
            stream = streams.queryBuilder().where()
                    .eq(Stream.NAME_FIELD, new SelectArg(name)).queryForFirst();

            if (stream == null) {
                Log.w("Stream.getByName",
                        "We received a stream message for a stream we don't have data for. Fake it until you make it.");
                stream = new Stream(name);
                app.getDao(Stream.class).createIfNotExists(stream);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return stream;
    }

    public static Stream getById(ZulipApp app, int id) {
        try {
            Dao<Stream, Integer> streams = app.getDatabaseHelper().getDao(
                    Stream.class);
            return streams.queryForId(id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public int getParsedColor() {
        if (!TextUtils.isEmpty(fetchedColor)) {
            parsedColor = parseColor(fetchedColor);
        }
        return parsedColor;
    }

    public Boolean getInHomeView() {
        return inHomeView;
    }

    public Boolean getInviteOnly() {
        return inviteOnly;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean isSubscribed) {
        subscribed = isSubscribed;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(name).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Stream rhs = (Stream) obj;
        return new EqualsBuilder().append(this.name, rhs.name).isEquals();
    }

    public int getId() {
        return id;
    }

    /**
     * Checks stream name is valid or not
     * @param app ZulipApp
     * @param streamName Checks this stream name is valid or not
     * @return null if stream does not exist else cursor
     */
    public static Stream streamCheckBeforeMessageSend(ZulipApp app, CharSequence streamName) {
        if (streamName == null) {
            return null;
        }
        try {
            return app.getDao(Stream.class)
                    .queryBuilder().where()
                    .eq(Stream.NAME_FIELD, new SelectArg(Stream.NAME_FIELD, streamName)).queryForFirst();
        } catch (SQLException e) {
            ZLog.logException(e);
        }
        return null;
    }

    public void setFetchColor(String fetchedColor) {
        this.fetchedColor = fetchedColor;
        getParsedColor();
    }

    public void setInHomeView(boolean inHomeView) {
        this.inHomeView = inHomeView;
    }
}