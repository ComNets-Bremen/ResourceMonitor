package de.uni_bremen.comnets.resourcemonitor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.AbstractResourceBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.BroadcastReceiverDescriptor;

public class MainActivity extends AppCompatActivity
                implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int ROUND_DECIMAL_PLACES = 1;  // Round decimal numbers
    private static final int USER_MANUAL_VERSION = 1;   // Increased if the manual is updated and should be shown again to the user

    private BroadcastReceiver updateUiBroadcastReceiver = null;

    MonitorService mService;
    boolean mBound = false;

    SharedPreferences preferences = null;

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

        // Remove boolean value for first start. We now use an int and we don't want to keep unused prefs...
        if (preferences.contains("firstStart")){
            preferences.edit().remove("firstStart").apply();
        }

        // Show dialog only on first startup
        if (preferences.getInt("user_manual_version", 0) < USER_MANUAL_VERSION){
            preferences.edit().putInt("user_manual_version", USER_MANUAL_VERSION).apply();
            showManualDialog();
        }

        updateUiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Ensure background service is up and running
        Intent intent = new Intent(this, MonitorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Receive broadcasts to update the UI
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MonitorService.UPLOAD_DONE_BROADCAST_ACTION);
        registerReceiver(updateUiBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(updateUiBroadcastReceiver);
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
        checkForFailedUploads();
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
                        getString(R.string.txt_version_code) + ": " + BuildConfig.VERSION_CODE + "\n\n" +
                        getString(R.string.txt_failed_uploads) + ": " + preferences.getInt(ExportDatabaseToServerTask.FAILED_UPLOADS, 0);
                Helper.showUserMessage(this, s, getString(R.string.txt_version_title));
                return true;
            case R.id.action_info:
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.infotext);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    StringBuilder stringBuilder = new StringBuilder(new String(b));
                    stringBuilder.append("<h2>" + getString(R.string.broadcast_module_info_title) + "</h2>");
                    stringBuilder.append(getString(R.string.broadcast_module_info_description));

                    HashMap<String, BroadcastReceiverDescriptor> receivers = AbstractResourceBroadcastReceiver.getRegisteredReceivers();

                    for (String key : receivers.keySet()){
                        stringBuilder.append("<h3>").append(receivers.get(key).getTitle()).append("</h3>");
                        stringBuilder.append("<p>").append(receivers.get(key).getDescription()).append("</p>");
                    }

                    text = stringBuilder.toString();


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

            case R.id.action_license:
                //showLicenseDialog();
                Intent intent = new Intent(this, OssLicensesMenuActivity.class);
                String title = getString(R.string.dialog_licenses_title);
                intent.putExtra("title", title);
                startActivity(intent);

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
        new ExportDatabaseToFileproviderTask(this, this, mService, intent, getResources().getText(R.string.dialog_export_provider).toString()).execute();
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

        new ExportDatabaseToFileproviderTask(this, this, mService, emailTxIntent, getResources().getText(R.string.dialog_mail_provider).toString()).execute();
    }

    /**
     * Export the database to the ComNets server
     */
    private void exportDatabaseToServer(){
        //new ExportDatabaseToServerTask(this, this, mService, false).execute();
        mService.uploadToServer(this);
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
            tvDischargeTotalPercentage.setText(getString(R.string.info_discharge_unit, String.valueOf(dischargeTotal)));

            TextView tvDischargeTotalTime = (TextView) findViewById(R.id.dischargeTableTotalTime);
            tvDischargeTotalTime.setText(String.valueOf(Math.round(dischargeTimePercentageTotal*100.0) + " %"));

            SimplePieChart svDischargeTimeTotal = (SimplePieChart) findViewById(R.id.dischargeTableTotalSurface);
            svDischargeTimeTotal.setPercentage(dischargeTimePercentageTotal);


            // Last 7 days
            List<BatteryChangeObject> discharge7dList = mService.getBatteryStats(System.currentTimeMillis()-60*60*24*7*1000, -1);
            double discharge7d = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(discharge7dList), ROUND_DECIMAL_PLACES);
            //String dischargeTime7d = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(discharge7dList), true, this);
            float dischargeTimePercentage7d = (float) BatteryChangeObject.getTotalDischargeTime(discharge7dList) / (float) BatteryChangeObject.getTotalTime(discharge7dList);

            TextView tvDischarge7dPercentage = (TextView) findViewById(R.id.dischargeTable7dPercent);
            tvDischarge7dPercentage.setText(getString(R.string.info_discharge_unit, String.valueOf(discharge7d)));

            TextView tvDischarge7dTime = (TextView) findViewById(R.id.dischargeTable7dTime);
            tvDischarge7dTime.setText(String.valueOf(Math.round(dischargeTimePercentage7d*100.0) + " %"));

            SimplePieChart svDischargeTime7d = (SimplePieChart) findViewById(R.id.dischargeTable7dSurface);
            svDischargeTime7d.setPercentage(dischargeTimePercentage7d);

            // Last 24 hours
            List<BatteryChangeObject> discharge24hList = mService.getBatteryStats(System.currentTimeMillis()-60*60*24*1000, -1);
            double discharge24h = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(discharge24hList), ROUND_DECIMAL_PLACES);
            //String dischargeTime24h = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(discharge24hList), true, this);
            float dischargeTimePercentage24h = (float) BatteryChangeObject.getTotalDischargeTime(discharge24hList) / (float) BatteryChangeObject.getTotalTime(discharge24hList);

            TextView tvDischarge24hPercentage = (TextView) findViewById(R.id.dischargeTable24hPercent);
            tvDischarge24hPercentage.setText(getString(R.string.info_discharge_unit, String.valueOf(discharge24h)));

            TextView tvDischarge24hTime = (TextView) findViewById(R.id.dischargeTable24hTime);
            tvDischarge24hTime.setText(String.valueOf(Math.round(dischargeTimePercentage24h*100.0) + " %"));

            SimplePieChart svDischargeTime24h = (SimplePieChart) findViewById(R.id.dischargeTable24hSurface);
            svDischargeTime24h.setPercentage(dischargeTimePercentage24h);


            // Last 3 hours
            List<BatteryChangeObject> discharge3hList = mService.getBatteryStats(System.currentTimeMillis()-60*60*3*1000, -1);
            double discharge3h = Helper.decimalRound(-100.0 * BatteryChangeObject.averageDischarge(discharge3hList), ROUND_DECIMAL_PLACES);
            //String dischargeTime24h = Helper.timePeriodFormat(BatteryChangeObject.getTotalDischargeTime(discharge24hList), true, this);
            float dischargeTimePercentage3h = (float) BatteryChangeObject.getTotalDischargeTime(discharge3hList) / (float) BatteryChangeObject.getTotalTime(discharge3hList);

            TextView tvDischarge3hPercentage = (TextView) findViewById(R.id.dischargeTable3hPercent);
            tvDischarge3hPercentage.setText(getString(R.string.info_discharge_unit, String.valueOf(discharge3h)));

            TextView tvDischarge3hTime = (TextView) findViewById(R.id.dischargeTable3hTime);
            tvDischarge3hTime.setText(String.valueOf(Math.round(dischargeTimePercentage3h*100.0) + " %"));

            SimplePieChart svDischargeTime3h = (SimplePieChart) findViewById(R.id.dischargeTable3hSurface);
            svDischargeTime3h.setPercentage(dischargeTimePercentage3h);


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

            // Settings might have changed. Inform the background service if there are changes
            mService.updatedSetting("data_collection_enabled");
            mService.updatedSetting("show_notification_bar");
            mService.updatedSetting("automatic_data_upload");
            mService.updatedSetting("automatic_data_upload_only_on_unmetered_connection");
            mService.updatedSetting("automatic_data_upload_interval");
            TextView lastUpload = (TextView) findViewById(R.id.lastUpload);
            String lastTime = mService.getLastServerUploadTime();
            if (lastTime == null){
                lastTime = getString(R.string.export_time_never);
            }

            lastUpload.setText(getString(R.string.export_last_upload, lastTime, mService.getLastServerUploadStatusText()));

            TextView lastDataItem = (TextView) findViewById(R.id.lastDataCollected);
            String lastCollectedItem = mService.getLastDataItemStored();
            if (lastCollectedItem == null) {
                lastCollectedItem = getString(R.string.export_time_never);
            }
            lastDataItem.setText(getString(R.string.last_item_to_db, lastCollectedItem));

            TextView automaticUpload = (TextView) findViewById(R.id.automaticUploadEnabled);

            // Disable upload button if automatic upload is enabled
            Button exportButton = (Button) findViewById(R.id.exportButton);
            if (preferences.getBoolean("automatic_data_upload", true)){
                exportButton.setVisibility(View.GONE);
                automaticUpload.setText(R.string.ui_automatic_upload_enabled);
            } else{
                exportButton.setVisibility(View.VISIBLE);
                automaticUpload.setText(R.string.ui_automatic_upload_disabled);
            }

            // TODO: Update more settings?
            mService.updateNotification();
        }

    }

    /**
     * Check for failed uploads and shows a user message in case of too many
     */
    private void checkForFailedUploads() {
        if (preferences.getInt(ExportDatabaseToServerTask.FAILED_UPLOADS,0) > ExportDatabaseToServerTask.MAX_NUM_FAILED_UPLOADS){
            Helper.showUserMessage(
                    this,
                    getString(R.string.upload_failed_multiple_times),
                    getString(R.string.upload_failed_multiple_times_title)
            );
            preferences.edit().putInt(ExportDatabaseToServerTask.FAILED_UPLOADS, 0).apply();
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

