package com.zulip.android.util;

import android.os.Build;

import com.zulip.android.BuildConfig;

public class BuildHelper {

    private BuildHelper() {
    }

    public static boolean shouldLogToCrashlytics() {
        return !isEmulator() && !BuildConfig.DEBUG;
    }

    private static boolean isEmulator() {
        return Build.HARDWARE.contains("goldfish");
    }
}
