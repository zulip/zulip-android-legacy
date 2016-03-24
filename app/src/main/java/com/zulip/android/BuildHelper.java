package com.zulip.android;

import android.os.Build;

public class BuildHelper {

    public static boolean shouldLogToCrashlytics() {
        return !isEmulator() && !BuildConfig.DEBUG;
    }

    public static boolean isEmulator() {
        return Build.HARDWARE.contains("goldfish");
    }
}
