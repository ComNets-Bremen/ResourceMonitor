package de.uni_bremen.comnets.resourcemonitor;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;


public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public PreferenceScreen getPreferenceScreen() {
        return super.getPreferenceScreen();
    }

}
