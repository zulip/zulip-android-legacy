package com.zulip.android.util;

import android.app.Activity;
import android.content.Context;

import com.zulip.android.R;

/**
 * Default Activity transition animation.
 */

public class ActivityTransitionAnim {
    /**
     * All usages must pass an activity context to execute a transition.
     *
     * @param activityContext {@link Context}
     */
    public static void transition(Context activityContext) {
        // avoid invalid class cast exception
        if (activityContext instanceof Activity) {
            ((Activity) activityContext).overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
        }
    }
}
