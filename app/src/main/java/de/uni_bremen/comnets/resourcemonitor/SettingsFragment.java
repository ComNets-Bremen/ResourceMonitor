package de.uni_bremen.comnets.resourcemonitor;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

/**
 * Setting fragment to edit the app settings
 */
public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // RM Values for debugging
        //getPreferenceManager().getSharedPreferences().edit().remove("automatic_data_upload_interval").apply();

        addPreferencesFromResource(R.xml.preferences);
        // TODO: change for API level higher 26
/*

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Disable the notification bar for Android versions with Doze
            Preference notificationbar = this.findPreference("show_notification_bar");
            notificationbar.getPreferenceManager().getSharedPreferences().edit().putBoolean("show_notification_bar", true).apply();
            notificationbar.setShouldDisableView(true);
            notificationbar.setEnabled(false);
        }
*/

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public PreferenceScreen getPreferenceScreen() {
        return super.getPreferenceScreen();
    }

}
