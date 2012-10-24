package com.humbughq.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.LinearLayout;

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
            this.that.pointerPos = (new JSONObject(result)).getInt("pointer");
            Log.i("pointer", "Pointer moving to " + this.that.pointerPos);

            LinearLayout tile = this.that.messageTiles
                    .get(this.that.pointerPos);
            if (tile != null) {
                this.that.mainScroller.scrollTo(0,
                        tile.getTop() + tile.getHeight() / 2
                                - this.that.mainScroller.getHeight() / 2);
            } else {
                Log.e("pointer", "Could not find a tile for "
                        + this.that.pointerPos);
            }

        } catch (JSONException e) {
            Log.e("json", "parsing error");
            e.printStackTrace();
        }

    }
}