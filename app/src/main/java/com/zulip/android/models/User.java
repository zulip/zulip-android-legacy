package com.zulip.android.models;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "reactions_user")
public class User {
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
     * Construct an empty User object.
     */
    public User() {

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
}
