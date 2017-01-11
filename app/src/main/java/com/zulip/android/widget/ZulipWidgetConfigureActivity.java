package com.zulip.android.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.view.View;
import android.widget.Toast;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;

import static com.zulip.android.widget.WidgetPreferenceFragment.FROM_PREFERENCE;
import static com.zulip.android.widget.WidgetPreferenceFragment.INTERVAL_PREFERENCE;
import static com.zulip.android.widget.WidgetPreferenceFragment.TITLE_PREFRENCE;

/**
 * The configuration screen for the {@link ZulipWidget ZulipWidget} AppWidget.
 */
public class ZulipWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "com.zulip.android.widget.ZulipWidget";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public ZulipWidgetConfigureActivity() {
        super();
    }

    static void savePref(Context context, int appWidgetId, String preferenceKey, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME + appWidgetId, 0).edit();
        prefs.putString(preferenceKey + appWidgetId, text);
        prefs.apply();
    }

    static String loadPref(Context context, int appWidgetId, String preferenceKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME + appWidgetId, 0);
        return prefs.getString(preferenceKey + appWidgetId, null);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);
        setContentView(R.layout.zulip_widget_configure);
        if (ZulipApp.get().getApiKey() == null || ZulipApp.get().getApiKey().isEmpty()) {
            Toast.makeText(this, R.string.no_login_error_widget, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final WidgetPreferenceFragment widgetPreferenceFragment = new WidgetPreferenceFragment();
        getFragmentManager().beginTransaction().replace(R.id.widget_settings_fragment, widgetPreferenceFragment).commit();
        findViewById(R.id.widget_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context = ZulipWidgetConfigureActivity.this;
                savePref(context, mAppWidgetId, TITLE_PREFRENCE, ((EditTextPreference) widgetPreferenceFragment.findPreference(TITLE_PREFRENCE)).getText());
                savePref(context, mAppWidgetId, FROM_PREFERENCE, ((ListPreference) widgetPreferenceFragment.findPreference(FROM_PREFERENCE)).getValue());
                savePref(context, mAppWidgetId, INTERVAL_PREFERENCE, ((ListPreference) widgetPreferenceFragment.findPreference(INTERVAL_PREFERENCE)).getValue());
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ZulipWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        findViewById(R.id.widget_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
    }
}