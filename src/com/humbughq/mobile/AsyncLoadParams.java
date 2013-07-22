package com.humbughq.mobile;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class AsyncLoadParams extends HumbugAsyncPushTask {
    public AsyncLoadParams(HumbugActivity humbugActivity) {
        super(humbugActivity);
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

                HashMap<String, Stream> streamHash = new HashMap<String, Stream>();

                for (int i = 0; i < subscriptions.length(); i++) {
                    Stream stream = new Stream(
                            subscriptions.getJSONObject(i));
                    Log.d("msg", "" + stream);
                    streamHash.put(stream.getName(), stream);
                }
                this.context.streams = streamHash;
            } catch (JSONException e) {
                Log.e("json", "parsing error");
                e.printStackTrace();
            }
        }
        callback.onTaskComplete(result);
    }
}
