package com.zulip.android;

import com.crashlytics.android.Crashlytics;

public class ZLog {

    public static void logException(Throwable e) {
        if (BuildHelper.shouldLogToCrashlytics()) {
            e.printStackTrace();
        } else {
            Crashlytics.logException(e);
        }
    }

}
