package com.humbughq.mobile;

import java.sql.SQLException;

import org.json.JSONException;

import android.os.Parcelable;

import com.j256.ormlite.stmt.Where;

/**
 * Mutates a Where object to implement the desired narrowing filter.
 */
public interface NarrowFilter extends Parcelable {
    public Where<Message, Object> modWhere(Where<Message, Object> where)
            throws SQLException;

    public boolean matches(Message msg);

    public String getTitle();

    public String getSubtitle();

    public Stream getComposeStream();

    public String getComposePMRecipient();

    public String getJsonFilter() throws JSONException;
}
