package de.uni_bremen.comnets.resourcemonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.BluetoothBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.ByteCountBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.CellularBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.AirplaneModeBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.PowerBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.ScreenBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.WiFiBroadcastReceiver;

/**
 * Background service to collect all kind of mobile phone information to evaluate
 * resource managing technologies
 */
public class MonitorService extends Service {

    public static final String TAG = MonitorService.class.getSimpleName();

    NotificationManager mNM;
    SharedPreferences preferences;
    SQLiteDatabase writableDb;
    EnergyMonitorDbHelper energyMonitorDbHelper;

    private int NOTIFICATION_ID = R.string.MonitorServiceStarted;
    private boolean dataCollectionRunning = false;

    PowerBroadcastReceiver powerBroadcastReceiver = null;
    ScreenBroadcastReceiver screenBroadcastReceiver = null;
    WiFiBroadcastReceiver wiFiBroadcastReceiver = null;
    AirplaneModeBroadcastReceiver airplaneModeBroadcastReceiver = null;
    ByteCountBroadcastReceiver byteCountBroadcastReceiver = null;
    BluetoothBroadcastReceiver bluetoothBroadcastReceiver = null;
    CellularBroadcastReceiver cellularBroadcastReceiver = null;

    /**
     * Binder for the communication with the foreground activity
     */
    public class MonitorServiceBinder extends Binder {
        MonitorService getService() {
            return MonitorService.this;
        }
    }

    /**
     * Create service: Show notification to show the service is running and collecting data
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        energyMonitorDbHelper = new EnergyMonitorDbHelper(getApplicationContext());
        writableDb = energyMonitorDbHelper.getWritableDatabase();
        Log.d(TAG, "db path: " +writableDb.getPath());

        powerBroadcastReceiver = new PowerBroadcastReceiver(writableDb);
        screenBroadcastReceiver = new ScreenBroadcastReceiver(writableDb);
        wiFiBroadcastReceiver = new WiFiBroadcastReceiver(writableDb);
        airplaneModeBroadcastReceiver = new AirplaneModeBroadcastReceiver(writableDb);
        byteCountBroadcastReceiver = new ByteCountBroadcastReceiver(writableDb);
        bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(writableDb);
        cellularBroadcastReceiver = new CellularBroadcastReceiver(writableDb);

        boolean dataCollectionEnabled = preferences.getBoolean("data_collection_enabled", true);
        if(dataCollectionEnabled){
            startDataCollection();
        } else {
            // Should not be started but ensure all notifications are correct
            stopDataCollection();
        }
    }

    /**
     * Got start command
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " +  startId + ": " + intent);
        return START_STICKY;
    }


    /**
     * Stop service
     */
    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy()");
        stopDataCollection();
        energyMonitorDbHelper.close();
        mNM.cancelAll();
        //restartService();
    }


    /**
     * Return the binder reference
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new MonitorServiceBinder();


    /**
     * Show / update a notification
     * @param text
     */
    private void showNotification(String text) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class)
                , 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.smallicon)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(text)
                .setContentText(getText(R.string.MonitorServiceLabel))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();

        mNM.notify(NOTIFICATION_ID, notification);
    }


    /**
     * Handle updates from the UI
     */
    public void updatedSetting(String key){
        if (key.equals("data_collection_enabled")){
            boolean dataCollectionEnabled = preferences.getBoolean("data_collection_enabled", true);
            if (dataCollectionEnabled && !dataCollectionRunning){
                startDataCollection();
            } else if (!dataCollectionEnabled && dataCollectionRunning){
                stopDataCollection();
            }
        }
    }

    /**
     * Return a string with some DB statistics
     * @return
     */
    String getDbStats(){
        return EnergyMonitorDbHelper.getDbStatistics(writableDb);
    }

    /**
     * Exports the complete database to a json object.
     *
     * @return JSONObject database as json
     */
    public JSONObject db2Json(){
        JSONObject jsonObject = new JSONObject();
        Cursor  batteryStatusCursor = writableDb.query(
                EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME,
                null, null,null, null, null, null);
        Cursor  byteCountCursor = writableDb.query(
                EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME,
                null, null,null, null, null, null);
        Cursor  bluetoothCursor = writableDb.query(
                EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME,
                null, null,null, null, null, null);
        Cursor  flightModeCursor = writableDb.query(
                EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME,
                null, null,null, null, null, null);
        Cursor  wifiCursor = writableDb.query(
                EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME,
                null, null,null, null, null, null);
        Cursor  screenStatusCursor = writableDb.query(
                EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME,
                null, null,null, null, null, null);

        try {
            jsonObject.put(EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(batteryStatusCursor));
            jsonObject.put(EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(byteCountCursor));
            jsonObject.put(EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(bluetoothCursor));
            jsonObject.put(EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(flightModeCursor));
            jsonObject.put(EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(wifiCursor));
            jsonObject.put(EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(screenStatusCursor));

            String uuid = preferences.getString("uuid", "None");
            String timezone = TimeZone.getDefault().getID();

            jsonObject.put("UUID", uuid);
            jsonObject.put("TIMEZONE", timezone);

            Log.d(TAG, "UUID: " +  uuid + " timezone: " + timezone);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    /**
     * Export the db json object via mail
     */
    public void exportData(){

        JSONObject jsonObject = db2Json();

        try {
            File f = new File(getFilesDir(), getString(R.string.export_filename));
            FileOutputStream fos = openFileOutput(f.getName(), Context.MODE_PRIVATE);
            GZIPOutputStream gos = new GZIPOutputStream(fos);
            gos.write(jsonObject.toString().getBytes());
            gos.close();
            fos.close();

            ArrayList<Uri> uris = new ArrayList<>();

            // Use a file provider to access the exported data via 3rd party app
            uris.add(FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileprovider", f));

            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.mail_destination)});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.mail_subject));
            emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_message));
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            try {
                startActivity(Intent.createChooser(emailIntent, getResources().getText(R.string.dialog_mail_provider)));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.dialog_no_mail_client), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register the broadcast receiver to start the data collection
     */
    public void startDataCollection(){
        showNotification(getString(R.string.DataCollectionRunning));
        powerBroadcastReceiver.register(this);
        screenBroadcastReceiver.register(this);
        wiFiBroadcastReceiver.register(this);
        airplaneModeBroadcastReceiver.register(this);
        byteCountBroadcastReceiver.register(this);
        bluetoothBroadcastReceiver.register(this);
        cellularBroadcastReceiver.register(this);
        dataCollectionRunning = true;

    }

    /**
     * Stop the data collection by unregistering the broadcast receiver
     */
    public void stopDataCollection(){
        showNotification(getString(R.string.DataCollectionStopped));
        powerBroadcastReceiver.unregister(this);
        screenBroadcastReceiver.unregister(this);
        wiFiBroadcastReceiver.unregister(this);
        airplaneModeBroadcastReceiver.unregister(this);
        byteCountBroadcastReceiver.unregister(this);
        bluetoothBroadcastReceiver.unregister(this);
        cellularBroadcastReceiver.unregister(this);
        dataCollectionRunning = false;
    }

    /**
     * Restart the service after the destroy was called to ensure it is continuously running
     */
    private void restartService(){
        Log.d(TAG, "Restart intent");
        Toast.makeText(this, "Sending restart intend", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("de.uni_bremen.comnets.resourcemonitor.restart");
        sendBroadcast(intent);
    }
}
