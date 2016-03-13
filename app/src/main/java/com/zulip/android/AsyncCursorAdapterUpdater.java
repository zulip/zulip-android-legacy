package com.zulip.android;

import java.util.concurrent.Callable;

import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.SimpleCursorAdapter;

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
