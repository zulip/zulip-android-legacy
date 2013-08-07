package com.humbughq.mobile;

import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.j256.ormlite.dao.Dao;

public class AsyncLoadParams extends HumbugAsyncPushTask {
    public AsyncLoadParams(HumbugActivity humbugActivity) {
        super(humbugActivity.app);
    }

    public final void execute() {
        execute("GET", "v1/users/me/subscriptions");
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            try {
                JSONArray subscriptions = new JSONObject(result)
                        .getJSONArray("subscriptions");

                Dao<Stream, String> streamDao;
                try {
                    streamDao = this.app.getDatabaseHelper().getDao(
                            Stream.class);
                } catch (SQLException e) {
                    // Well that's sort of awkward. We can't really store this
                    // data except in the database.
                    e.printStackTrace();
                    Log.e("ALP", "Could not initialise Stream database");
                    return;
                }

                for (int i = 0; i < subscriptions.length(); i++) {
                    Stream stream = new Stream(subscriptions.getJSONObject(i));
                    Log.d("msg", "" + stream);

                    try {
                        streamDao.createOrUpdate(stream);
                    } catch (SQLException e) {
                        // This isn't totally fatal, because while a lack of
                        // stream data is depressing our app will still
                        // mostly function without it.
                        Log.e("ALP",
                                "Could not create or update stream in database.");
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            }
        }
        callback.onTaskComplete(result);
    }
}
