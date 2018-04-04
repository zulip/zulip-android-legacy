package com.zulip.android.filters;

import android.os.Parcel;

import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import com.zulip.android.database.DatabaseHelper;
import com.zulip.android.models.Message;
import com.zulip.android.models.Stream;

import org.json.JSONArray;
import org.json.JSONException;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Narrow based on search terms
 * {@link NarrowFilterSearch#query} is the search query
 * {@link NarrowFilterSearch#filter} indicates the current narrow filter
 */
public class NarrowFilterSearch implements NarrowFilter {
    public static final Creator<NarrowFilterSearch> CREATOR = new Creator<NarrowFilterSearch>() {
        @Override
        public NarrowFilterSearch createFromParcel(Parcel parcel) {
            return new NarrowFilterSearch(parcel.readString(), null);
        }

        @Override
        public NarrowFilterSearch[] newArray(int i) {
            return new NarrowFilterSearch[i];
        }
    };

    private String query;
    private NarrowFilter filter;

    public NarrowFilterSearch(String query, NarrowFilter filter) {
        this.query = query;
        this.filter = filter;
    }

    @Override
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException {
        where.raw(
                Message.CONTENT_FIELD + " LIKE ? COLLATE NOCASE",
                new SelectArg(Message.CONTENT_FIELD, "%"
                        + DatabaseHelper.likeEscape(query) + "%"));
        return where;
    }

    @Override
    public boolean matches(Message msg) {
        // api gives a list of messages matching the search query
        // when in home view, we want to show all the matching results
        // when narrowed, we want to show messages which match the current narrow of user
        return filter == null || filter.matches(msg);
    }

    @Override
    public String getTitle() {
        return "Searching '" + query + "'";
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public Stream getComposeStream() {
        return null;
    }

    @Override
    public String getComposePMRecipient() {
        return null;
    }

    @Override
    public String getJsonFilter() throws JSONException {
        JSONArray filter = new JSONArray();
        filter.put(new JSONArray(Arrays.asList("search", query)));
        return filter.toString();
    }

    @Override
    public boolean equals(NarrowFilter filter) {
        if (filter instanceof NarrowFilterSearch) {
            NarrowFilterSearch filterSearch = (NarrowFilterSearch) filter;
            return this.getTitle().equals(filterSearch.getTitle());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        try {
            return getJsonFilter();
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(query);
    }
}
