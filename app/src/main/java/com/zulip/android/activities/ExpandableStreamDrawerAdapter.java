package com.zulip.android.activities;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.widget.SimpleCursorTreeAdapter;

import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.util.ZLog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the left side drawer of the streams and subjects.
 */
public class ExpandableStreamDrawerAdapter extends SimpleCursorTreeAdapter {

    public ExpandableStreamDrawerAdapter(final Context context, Cursor cursor, int groupLayout,
                                         String[] groupFrom, int[] groupTo, int childLayout,
                                         String[] childFrom, int[] childTo) {
        super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
    }

    @Override
    public Cursor getChildrenCursor(Cursor groupCursor) {
        List<String[]> results = new ArrayList<>();
        try {
            results = ZulipApp.get().getDao(Message.class).queryRaw("SELECT DISTINCT subject FROM messages " +
                    "JOIN streams ON streams.id=messages.stream " +
                    "WHERE streams.id=" + groupCursor.getInt(0)).getResults();
        } catch (SQLException e) {
            ZLog.logException(e);
        }
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"subject", "_id"});

        for (String[] result : results) {
            try {
                matrixCursor.addRow(new String[]{result[0], String.valueOf(groupCursor.getInt(0))});
            } catch (Exception e) {
                ZLog.logException(e);
            }
        }
        return matrixCursor;
    }
}