package de.uni_bremen.comnets.resourcemonitor;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
                implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int ROUND_DECIMAL_PLACES = 1;

    MonitorService mService;
    boolean mBound = false;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Add the setting fragment to the main activity
        //getFragmentManager().beginTransaction().add(R.id.linearLayout, new SettingsFragment()).commit();

        // Generate a random UUID and store it to the preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("uuid", null) == null)
            preferences.edit().putString("uuid", UUID.randomUUID().toString()).apply();
        Log.d(TAG, "UUID: " + preferences.getString("uuid", "NONE"));

        // We removed the opportunity to disable the data collection. Ensure that it is always enabled.
        // TODO: Remove after some updates as it should be always switched on

        if (!preferences.getBoolean("data_collection_enabled", true)){
            preferences.edit().putBoolean("data_collection_enabled", true).apply();
        }

        // Ensure background service is running
        Intent serviceIntent = new Intent(this, MonitorService.class);
        startService(serviceIntent);

        Button exportButton = (Button) findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (mBound){
                    //exportDatabaseToMail();
                    exportDatabaseToServer();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.warn_connect_service), Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView uuidText = (TextView) findViewById(R.id.uuidText);
        uuidText.setText((uuidText.getText() + " " + preferences.getString("uuid", "None")));

        // Show dialog only on first startup
        if (preferences.getBoolean("firstStart", true)){
            preferences.edit().putBoolean("firstStart", false).apply();
            showManualDialog();
        }

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
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String text;
        switch (item.getItemId()) {
            case R.id.action_contact:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto",getString(R.string.mail_developer), null));
                String body = "\n\n" + "--" + "\n" +
                        getString(R.string.mail_developer_body) + "\n\n" +
                        "Version_Code: " + BuildConfig.VERSION_CODE + "\n" +
                        "Version_Name: " + BuildConfig.VERSION_NAME + "\n" +
                        "Language: " + Locale.getDefault().toString() + "\n" +
                        "Android: " + Build.VERSION.RELEASE;
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_developer_subject) + ": " + getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.dialog_mail_provider)));
                return true;
            case R.id.action_export_file:
                exportDatabaseToAnywhere();
                return true;
            case R.id.action_github:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_page)));
                startActivity(browserIntent);
                return true;
            case R.id.action_version:
                String s = getString(R.string.txt_version_name) + ": " + BuildConfig.VERSION_NAME + "\n" +
                        getString(R.string.txt_version_code) + ": " + BuildConfig.VERSION_CODE;
                Helper.showUserMessage(this, s, getString(R.string.txt_version_title));
                return true;
            case R.id.action_info:
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.infotext);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                Helper.showHTMLUserMessage(this, text, getResources().getString(R.string.dialog_help_title));
                return true;

            case R.id.action_about_developer:
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.about);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                Helper.showHTMLUserMessage(this, text, getResources().getString(R.string.dialog_help_title));

                return true;

            case R.id.action_manual:
                showManualDialog();
                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show the dialog telling the user how to use this app
     */
    private void showManualDialog() {
        String text;
        try {
            InputStream ioHelp = getResources().openRawResource(R.raw.manual);
            byte[] b = new byte[ioHelp.available()];
            ioHelp.read(b);
            text = new String(b);
        } catch (IOException e) {
            text = getResources().getString(R.string.dialog_not_available);
        }

        Helper.showHTMLUserMessage(this, text, getResources().getString(R.string.dialog_help_title));
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorService.MonitorServiceBinder binder = (MonitorService.MonitorServiceBinder) service;
            mService = binder.getService();
            mBound = true;

            updateUi();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mService != null) {
            mService.updatedSetting(key);
        }
    }


    /**
     * Export the database to anywhere using a thread to keep UI responding
     */
    private void exportDatabaseToAnywhere(){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        new ExportDatabaseTask(this, this, mService, intent, getResources().getText(R.string.dialog_export_provider).toString()).execute();
    }

    /**
     * Export the database via mail using a thread to keep UI responding
     */
    private void exportDatabaseToMail(){
        Intent emailTxIntent = new Intent(Intent.ACTION_SEND);
        emailTxIntent.setType("message/rfc822");
        emailTxIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.mail_destination)});
        emailTxIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.mail_subject));
        emailTxIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_message));

        new ExportDatabaseTask(this, this, mService, emailTxIntent, getResources().getText(R.string.dialog_mail_provider).toString()).execute();
    }

    /**
     * Export the database to the ComNets server
     */
    private void exportDatabaseToServer(){
        new ExportDatabaseToServerTask(this, this, mService, false).execute();
    }

    /**
     * Update the user interface after changes
     */
    private void updateUi() {
        if (mBound) {

            // Total time
            List<BatteryChangeObject> dischargeTotalList = mService.getBatteryStats(-1, -1);
            double dischargeTotal = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(dischargeTotalList), ROUND_DECIMAL_PLACES);
            //String dischargeTimeTotal = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(dischargeTotalList), true, this);
            float dischargeTimePercentageTotal = (float) BatteryChangeObject.getTotalDischargeTime(dischargeTotalList) / (float) BatteryChangeObject.getTotalTime(dischargeTotalList);

            TextView tvDischargeTotalPercentage = (TextView) findViewById(R.id.dischargeTableTotalPercent);
            tvDischargeTotalPercentage.setText(String.valueOf(dischargeTotal) + " " + getString(R.string.info_discharge_unit));

            TextView tvDischargeTotalTime = (TextView) findViewById(R.id.dischargeTableTotalTime);
            tvDischargeTotalTime.setText(String.valueOf(Math.round(dischargeTimePercentageTotal*100.0) + " %"));

            SurfaceView svDischargeTimeTotal = (SurfaceView) findViewById(R.id.dischargeTableTotalSurface);
            Helper.setPercentageOnSurfaceView(svDischargeTimeTotal, dischargeTimePercentageTotal);




            // Last 7 days
            List<BatteryChangeObject> discharge7dList = mService.getBatteryStats(System.currentTimeMillis()-60*60*24*7*1000, -1);
            double discharge7d = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(discharge7dList), ROUND_DECIMAL_PLACES);
            //String dischargeTime7d = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(discharge7dList), true, this);
            float dischargeTimePercentage7d = (float) BatteryChangeObject.getTotalDischargeTime(discharge7dList) / (float) BatteryChangeObject.getTotalTime(discharge7dList);

            TextView tvDischarge7dPercentage = (TextView) findViewById(R.id.dischargeTable7dPercent);
            tvDischarge7dPercentage.setText(String.valueOf(discharge7d) + " " + getString(R.string.info_discharge_unit));

            TextView tvDischarge7dTime = (TextView) findViewById(R.id.dischargeTable7dTime);
            tvDischarge7dTime.setText(String.valueOf(Math.round(dischargeTimePercentage7d*100.0) + " %"));

            SurfaceView svDischargeTime7d = (SurfaceView) findViewById(R.id.dischargeTable7dSurface);
            Helper.setPercentageOnSurfaceView(svDischargeTime7d, dischargeTimePercentage7d);


            // Last 24 hours
            List<BatteryChangeObject> discharge24hList = mService.getBatteryStats(System.currentTimeMillis()-60*60*24*1000, -1);
            double discharge24h = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(discharge24hList), ROUND_DECIMAL_PLACES);
            //String dischargeTime24h = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(discharge24hList), true, this);
            float dischargeTimePercentage24h = (float) BatteryChangeObject.getTotalDischargeTime(discharge24hList) / (float) BatteryChangeObject.getTotalTime(discharge24hList);

            TextView tvDischarge24hPercentage = (TextView) findViewById(R.id.dischargeTable24hPercent);
            tvDischarge24hPercentage.setText(String.valueOf(discharge24h) + " " + getString(R.string.info_discharge_unit));

            TextView tvDischarge24hTime = (TextView) findViewById(R.id.dischargeTable24hTime);
            tvDischarge24hTime.setText(String.valueOf(Math.round(dischargeTimePercentage24h*100.0) + " %"));

            SurfaceView svDischargeTime24h = (SurfaceView) findViewById(R.id.dischargeTable24hSurface);
            Helper.setPercentageOnSurfaceView(svDischargeTime24h, dischargeTimePercentage24h);

            // Last 3 hours
            List<BatteryChangeObject> discharge3hList = mService.getBatteryStats(System.currentTimeMillis()-60*60*3*1000, -1);
            double discharge3h = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(discharge3hList), ROUND_DECIMAL_PLACES);
            //String dischargeTime24h = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(discharge24hList), true, this);
            float dischargeTimePercentage3h = (float) BatteryChangeObject.getTotalDischargeTime(discharge3hList) / (float) BatteryChangeObject.getTotalTime(discharge3hList);

            TextView tvDischarge3hPercentage = (TextView) findViewById(R.id.dischargeTable3hPercent);
            tvDischarge3hPercentage.setText(String.valueOf(discharge3h) + " " + getString(R.string.info_discharge_unit));

            TextView tvDischarge3hTime = (TextView) findViewById(R.id.dischargeTable3hTime);
            tvDischarge3hTime.setText(String.valueOf(Math.round(dischargeTimePercentage3h*100.0) + " %"));

            SurfaceView svDischargeTime3h = (SurfaceView) findViewById(R.id.dischargeTable3hSurface);
            Helper.setPercentageOnSurfaceView(svDischargeTime3h, dischargeTimePercentage3h);


            ImageView allTime = (ImageView) findViewById(R.id.ui_thumb_allTime);

            if (discharge24h <= dischargeTotal) {
                allTime.setImageResource(R.drawable.thumb_green);
            } else {
                allTime.setImageResource(R.drawable.thumb_red);
            }

            ImageView last7days = (ImageView) findViewById(R.id.ui_thumb_7d);

            if (discharge24h <= discharge7d) {
                last7days.setImageResource(R.drawable.thumb_green);
            } else {
                last7days.setImageResource(R.drawable.thumb_red);
            }


            /*

            ImageView emotionView = (ImageView) findViewById(R.id.emotionView);
            emotionView.setImageResource(R.drawable.happy);
             */
            mService.updatedSetting("data_collection_enabled");
            mService.updatedSetting("show_notification_bar");
            TextView lastUpload = (TextView) findViewById(R.id.lastUpload);
            String lastTime = mService.getLastServerUploadTime();
            if (lastTime == null){
                lastTime = getString(R.string.export_time_never);
            }
            lastUpload.setText(getString(R.string.export_last_upload) +  ": " + lastTime);

            TextView lastDataItem = (TextView) findViewById(R.id.lastDataCollected);
            String lastCollectedItem = mService.getLastDataItemStored();
            if (lastCollectedItem == null) {
                lastCollectedItem = getString(R.string.export_time_never);
            }
            lastDataItem.setText(getString(R.string.last_item_to_db) + ": " + lastCollectedItem);

            mService.updateNotification();
            // TODO: Update more settings?
        }

    }

/*
    private void requestDozePermissions(){
        if (Helper.isPowerSaving(this) &&
                !preferences.getBoolean("show_notification_bar", true)
                ) {
            // User requested to disable icon without adding doze exception for this app. We should inform him how to do that

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {

                String text = "";

                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.doze_info);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                };

                Helper.showHTMLUserMessage(this, text, getString(R.string.dialog_help_title), listener);


                preferences.edit().putBoolean("show_notification_bar", true).apply();
                mService.updatedSetting("show_notification_bar");
            }
        }
    }
    */

}

