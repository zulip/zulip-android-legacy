package com.zulip.android.util;

import android.util.Log;

import com.zulip.android.util.BuildHelper;

public class ZLog {

    private ZLog() {}

    public static void logException(Throwable e) {
        if (!BuildHelper.shouldLogToCrashlytics()) {
            Log.e("Error", "oops", e);
        } else {
            // TODO(lfaraone): figure out what to do about crash reporting
        }
    }

}
