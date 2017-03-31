package com.zulip.android.models;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "reactions_user")
public class UserReaction {
    private static final String ID_FIELD = "id";
    private static final String NAME_FIELD = "name";
    private static final String EMAIL_FIELD = "email";

    @SerializedName("id")
    @DatabaseField(id = true, columnName = ID_FIELD)
    private int id;

    @SerializedName("full_name")
    @DatabaseField(columnName = NAME_FIELD)
    private String name;

    @SerializedName("email")
    @DatabaseField(columnName = EMAIL_FIELD)
    private String email;

    /**
     * Used in reaction type event {@link com.zulip.android.networking.response.events.ReactionWrapper}
     */
    @SerializedName("user_id")
    private int alternateId;

    /**
     * Construct an empty User object.
     */
    public UserReaction() {

    }


    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public int getAlternateId() {
        return this.alternateId;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserReaction) {
            UserReaction user = (UserReaction) obj;
            return this.getId() == user.getId();
        } else {
            return false;
        }
    }
}
