package com.zulip.android;

import android.os.Build;

public class ZLog {

    public static void logException(Throwable e) {
        if (Build.HARDWARE.contains("goldfish")) {
            e.printStackTrace();
        } else {
            //Crashlytics.logException(e);
        }
    }

}
