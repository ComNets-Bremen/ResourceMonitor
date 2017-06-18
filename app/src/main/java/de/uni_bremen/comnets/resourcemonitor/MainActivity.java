package de.uni_bremen.comnets.resourcemonitor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
                implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    MonitorService mService;
    ProgressDialog mProgressDialog;
    boolean mBound = false;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Add the setting fragment to the main activity
        getFragmentManager().beginTransaction().add(R.id.linearLayout, new SettingsFragment()).commit();

        // Generate a random UUID and store it to the preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("uuid", null) == null)
            preferences.edit().putString("uuid", UUID.randomUUID().toString()).apply();
        Log.d(TAG, "UUID: " + preferences.getString("uuid", "NONE"));

        // Ensure background service is running
        Intent serviceIntent = new Intent(this, MonitorService.class);
        startService(serviceIntent);

        Button aboutButton = (Button) findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                String text;
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.infotext);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                showUserMessage(Html.fromHtml(text), getResources().getString(R.string.dialog_help_title));
            }
        });

        Button infoButton = (Button) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                String text;
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.about);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                showUserMessage(Html.fromHtml(text), getResources().getString(R.string.dialog_help_title));
            }
        });

        Button exportButton = (Button) findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (mBound){
                    exportDatabase();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.warn_connect_service), Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView uuidText = (TextView) findViewById(R.id.uuidText);
        uuidText.setText((uuidText.getText() + " " + preferences.getString("uuid", "None")));

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MonitorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);
        TextView tv = (TextView) findViewById(R.id.statusText);
        if (mBound) {
            tv.setText(mService.getDbStats());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorService.MonitorServiceBinder binder = (MonitorService.MonitorServiceBinder) service;
            mService = binder.getService();
            mBound = true;

            TextView tv = (TextView) findViewById(R.id.statusText);
            tv.setText(mService.getDbStats());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mService.updatedSetting(key);
    }

    /**
     * Show a Message dialog to the user
     * @param s     The message as a String object
     * @param title The title of the message box
     */
    private void showUserMessage(String s, String title) {
        Spanned span = new SpannedString(s);
        showUserMessage(span, title);
    }

    /**
     * Show a Message dialog to the user
     * @param msg   The message as a Spanned object
     * @param title The title of the message box
     */
    private void showUserMessage(Spanned msg, String title) {
        AlertDialog dlgAlert = new AlertDialog.Builder(this)
                .setTitle(title)
                .setPositiveButton(getText(R.string.button_ok), null)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_info)
                .create();
        dlgAlert.show();
        TextView tv = (TextView) dlgAlert.findViewById(android.R.id.message);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setClickable(true);
    }

    /**
     * Export the database using a thread to keep UI responsing
     */
    private void exportDatabase(){
        mProgressDialog = ProgressDialog.show(this, getString(R.string.export_progress_title),getString(R.string.export_progress_text), true);
        new Thread() {
            @Override
            public void run() {

                mService.exportData();
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                        }
                    });
                } catch (final Exception e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.export_failed), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }.start();
    }
}

