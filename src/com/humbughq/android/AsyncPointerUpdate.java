package com.humbughq.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class AsyncPointerUpdate extends HumbugAsyncPushTask {

    public AsyncPointerUpdate(HumbugActivity humbugActivity) {
        super(humbugActivity);
    }

    public final void execute() {
        execute("api/v1/get_profile");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            this.context.mainScroller.select((new JSONObject(result))
                    .getInt("pointer"));

        } catch (JSONException e) {
            Log.e("json", "parsing error");
            e.printStackTrace();
        }

    }
}