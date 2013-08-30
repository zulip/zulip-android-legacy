package com.humbughq.mobile;

import java.util.concurrent.Callable;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;

public class RefreshableCursorAdapter extends SimpleCursorAdapter {

    private Callable<Cursor> cursorGenerator;

    public RefreshableCursorAdapter(Context context, int layout,
            Cursor initialCursor, Callable<Cursor> cursorGenerator,
            String[] from, int[] to, int flags) {
        super(context, layout, initialCursor, from, to, flags);
        this.cursorGenerator = cursorGenerator;
    }

    public void refresh() {
        this.changeCursor(new MatrixCursor(this.getCursor().getColumnNames()));
        this.notifyDataSetChanged();
        (new AsyncCursorAdapterUpdater()).execute(cursorGenerator, this);

    }
}
