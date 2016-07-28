package com.zulip.android.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "emoji")
public class Emoji {
    /**
     * Name field should be the same as {@link Person#NAME_FIELD}
     */
    public static final String NAME_FIELD = "name";

    @DatabaseField(columnName = NAME_FIELD)
    private String name;

    public Emoji() {
    }

    public Emoji(String emoji) {
        this.name = emoji;
    }
}
