package com.zulip.android;

import java.util.concurrent.Callable;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;

/**
 * An adapter that can be refreshed by any object that has a reference to it.
 * 
 * This is useful when you don't want to have to encode knowledge of how to
 * refresh an adapter into any function that might want to do so.
 */
public class RefreshableCursorAdapter extends SimpleCursorAdapter {

    private Callable<Cursor> cursorGenerator;

    public RefreshableCursorAdapter(Context context, int layout,
            Cursor initialCursor, Callable<Cursor> cursorGenerator,
            String[] from, int[] to, int flags) {
        super(context, layout, initialCursor, from, to, flags);
        this.cursorGenerator = cursorGenerator;
    }

    /**
     * Asynchronously updates the CursorAdapter
     * 
     * Immediately on invocation, the CursorAdapter is blanked. It is updated
     * once the refresh completes.
     */
    public void refresh() {
        this.changeCursor(new MatrixCursor(this.getCursor().getColumnNames()));
        this.notifyDataSetChanged();
        (new AsyncCursorAdapterUpdater()).execute(cursorGenerator, this);

    }
}
