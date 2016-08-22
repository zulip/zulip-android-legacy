package com.zulip.android.networking;

import java.util.concurrent.Callable;

import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.SimpleCursorAdapter;

import com.zulip.android.util.ZLog;

/**
 * A background task which asynchronously updates data.
 * Used here{@link com.zulip.android.activities.RefreshableCursorAdapter} for updating the
 * online people in the person (right) drawer.
 */
public class AsyncCursorAdapterUpdater extends
        AsyncTask<String, String, Cursor> {

    private SimpleCursorAdapter adapter;
    private Callable<Cursor> cursorGenerator;

    public void execute(Callable<Cursor> cursorGenerator,
                        SimpleCursorAdapter adapter) {
        this.adapter = adapter;
        this.cursorGenerator = cursorGenerator;
        this.execute();
    }

    @Override
    protected Cursor doInBackground(String... params) {
        try {
            return this.cursorGenerator.call();
        } catch (Exception e) {
            ZLog.logException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Cursor result) {
        this.adapter.changeCursor(result);
    }

}
