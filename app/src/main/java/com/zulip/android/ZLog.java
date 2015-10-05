package com.zulip.android;

import android.os.Build;

import com.crashlytics.android.Crashlytics;

public class ZLog {

    public static void logException(Throwable e) {
        if (Build.HARDWARE.contains("goldfish")) {
            e.printStackTrace();
        } else {
            Crashlytics.logException(e);
        }
    }

}
