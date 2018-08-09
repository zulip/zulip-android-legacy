package com.zulip.android.util;

import android.app.Activity;
import android.content.Context;

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
            ((Activity) activityContext).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}
