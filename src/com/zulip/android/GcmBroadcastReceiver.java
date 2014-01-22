package com.zulip.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

// This class receives GCM messages from the cloud. It then passes them through a series of steps
// to get a notification on the menu bar. See
// http://commonsware.com/blog/2010/08/11/activity-notification-ordered-broadcast.html
// for the inspiration for the dispatch strategy.

// A com.humbughq.mobile.PushMessage.BROADCAST is broadcast to ordered receivers. If the
// HumbugActivity is active, its receiver gets the message first and inhibits it. Otherwise,
// it is received by GcmShowNotificationReceiver. That launches a GcmIntentService that
// displays the notification.

public class GcmBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = "GCM";
    // FIXME Not sure if this is safe to change to a Zulip string
    public static final String BROADCAST = "com.humbughq.mobile.PushMessage.BROADCAST";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) { // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that
             * GCM will be extended in the future with new message types, just
             * ignore any message types you're not interested in, or that you
             * don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
                    .equals(messageType)) {
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
                    .equals(messageType)) {
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
                    .equals(messageType)) {

                Log.i(TAG, "Received: " + extras.toString());
                if (extras.getString("event").equals("message")) {
                    Intent broadcast = new Intent(BROADCAST);
                    broadcast.putExtras(extras);
                    context.sendOrderedBroadcast(broadcast, null);
                }
            }
        }

        setResultCode(Activity.RESULT_OK);
    }
}
