package de.uni_bremen.comnets.resourcemonitor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Create an SettingsFragment Activity
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        setTitle(getText(R.string.action_settings));
        super.onCreate(savedInstanceState);
    }
}
