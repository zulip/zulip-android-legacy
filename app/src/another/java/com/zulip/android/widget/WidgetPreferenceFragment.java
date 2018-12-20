package com.zulip.android.widget;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.zulip.android.R;


public class WidgetPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String TITLE_PREFRENCE = "title_preference";
    public static final String FROM_PREFERENCE = "from_preference";
    public static final String INTERVAL_PREFERENCE = "interval_preference";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        findPreference(TITLE_PREFRENCE).setOnPreferenceChangeListener(this);
        findPreference(FROM_PREFERENCE).setOnPreferenceChangeListener(this);
        findPreference(INTERVAL_PREFERENCE).setOnPreferenceChangeListener(this);

        findPreference(TITLE_PREFRENCE).setSummary(((EditTextPreference) findPreference(TITLE_PREFRENCE)).getText());
        findPreference(FROM_PREFERENCE).setSummary(((ListPreference) findPreference(FROM_PREFERENCE)).getValue());
        findPreference(INTERVAL_PREFERENCE).setSummary(((ListPreference) findPreference(INTERVAL_PREFERENCE)).getValue());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {

        switch (preference.getKey()) {
            case TITLE_PREFRENCE:
                preference.setSummary(String.valueOf(object));
                return true;
            case FROM_PREFERENCE:
                preference.setSummary(String.valueOf(object));
                return true;
            case INTERVAL_PREFERENCE:
                preference.setSummary(String.valueOf(object));
                return true;
            default:
                break;
        }

        return false;
    }
}