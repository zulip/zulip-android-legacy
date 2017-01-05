package com.zulip.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.networking.AsyncGetEvents;

import static com.zulip.android.widget.WidgetPreferenceFragment.FROM_PREFERENCE;
import static com.zulip.android.widget.WidgetPreferenceFragment.INTERVAL_PREFERENCE;
import static com.zulip.android.widget.WidgetPreferenceFragment.TITLE_PREFRENCE;


public class ZulipWidget extends AppWidgetProvider {
    private static AsyncGetEvents asyncGetEvents;
    private static int intervalMilliseconds = 0;
    public static String WIDGET_REFRESH = "com.zulip.android.zulipwidget.REFRESH";

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

            if (asyncGetEvents == null) {
                setupGetEvents();
            }
            final Intent refreshIntent = new Intent(context, ZulipWidget.class);
            refreshIntent.setAction(ZulipWidget.WIDGET_REFRESH);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (asyncGetEvents == null) {
            setupGetEvents();
        }
        if (action.equals(WIDGET_REFRESH)) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, ZulipWidget.class);
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widget_list);
            asyncGetEvents.interrupt();
        }
        super.onReceive(context, intent);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private static void setupGetEvents() {
        asyncGetEvents = new AsyncGetEvents(intervalMilliseconds);
        asyncGetEvents.start();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
        asyncGetEvents = null;
    }
}

