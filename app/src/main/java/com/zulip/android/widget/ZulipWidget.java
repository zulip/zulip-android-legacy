package com.zulip.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;

import static com.zulip.android.widget.WidgetPreferenceFragment.FROM_PREFERENCE;
import static com.zulip.android.widget.WidgetPreferenceFragment.INTERVAL_PREFERENCE;
import static com.zulip.android.widget.WidgetPreferenceFragment.TITLE_PREFRENCE;


public class ZulipWidget extends AppWidgetProvider {
    private static int intervalMilliseconds = 0;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String title = ZulipWidgetConfigureActivity.loadPref(context, appWidgetId, TITLE_PREFRENCE);
        if (title != null) {
            String from = ZulipWidgetConfigureActivity.loadPref(context, appWidgetId, FROM_PREFERENCE);
            String interval = ZulipWidgetConfigureActivity.loadPref(context, appWidgetId, INTERVAL_PREFERENCE);
            intervalMilliseconds = 60000 * Integer.parseInt(TextUtils.substring(interval, 0, interval.length() - 1));
            // Construct the RemoteViews object
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.zulip_widget);
            final Intent intent = new Intent(context, ZulipWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(TITLE_PREFRENCE, title);
            intent.putExtra(FROM_PREFERENCE, from);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setTextViewText(R.id.widget_title, title);
            remoteViews.setRemoteAdapter(appWidgetId, R.id.widget_list, intent);
            remoteViews.setEmptyView(R.id.widget_list, R.id.widget_nomsg);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        super.onReceive(context, intent);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}

