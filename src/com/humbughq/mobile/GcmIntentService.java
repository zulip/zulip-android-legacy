package com.humbughq.mobile;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    public static final String TAG = "GCM";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) { // has effect of unparcelling Bundle
            Log.i(TAG, "Received a message: " + messageType);
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

                // Post notification of received message.
                sendNotification(extras);
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(Bundle msg) {
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HumbugActivity.class), 0);

        String type = msg.getString("recipient_type");
        String title = msg.getString("alert");
        String tag = "zulip";

        if (type.equals("private")) {
            title = msg.getString("sender_full_name");
            tag = "zulip-pm-" + msg.getString("sender_email");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setStyle(
                        new NotificationCompat.BigTextStyle().bigText(msg
                                .getString("content")));

        if (msg.containsKey("time")) {
            long time = Long.parseLong(msg.getString("time")) * 1000;
            Log.i("GCM", "time: " + time);
            builder.setWhen(time);
        }

        builder.setContentIntent(contentIntent);
        mNotificationManager.notify(tag, NOTIFICATION_ID, builder.build());
    }
}
