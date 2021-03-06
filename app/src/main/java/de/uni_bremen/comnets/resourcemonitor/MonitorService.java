package de.uni_bremen.comnets.resourcemonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.evernote.android.job.JobManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import androidx.work.WorkManager;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.AirplaneModeBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.BluetoothBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.ByteCountBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.CellularBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.DataUploadBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.PowerBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.ScreenBroadcastReceiver;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.WiFiBroadcastReceiver;

/**
 * Background service to collect all kind of mobile phone information to evaluate
 * resource managing technologies
 */
public class MonitorService extends Service {

    public static final String TAG = MonitorService.class.getSimpleName();
    public static final String UPLOAD_DONE_BROADCAST_ACTION = TAG + ".UPLOAD_DONE";
    private static final int NOTIFICATION_ID = R.string.MonitorServiceStarted;

    NotificationManager mNM = null;
    SharedPreferences preferences;
    SQLiteDatabase writableDb;
    static EnergyMonitorDbHelper energyMonitorDbHelper = null;
    String lastDataItemStored = null;

    private boolean dataCollectionRunning = false;

    PowerBroadcastReceiver powerBroadcastReceiver = null;
    ScreenBroadcastReceiver screenBroadcastReceiver = null;
    WiFiBroadcastReceiver wiFiBroadcastReceiver = null;
    AirplaneModeBroadcastReceiver airplaneModeBroadcastReceiver = null;
    ByteCountBroadcastReceiver byteCountBroadcastReceiver = null;
    BluetoothBroadcastReceiver bluetoothBroadcastReceiver = null;
    CellularBroadcastReceiver cellularBroadcastReceiver = null;

    DataUploadBroadcastReceiver dataUploadBroadcastReceiver = null;


    /**
     * Export data to json
     *
     * @param job The json object defining which data is already available at the server side
     * @return The missing data
     */
    public JSONObject exportDataForServer(JSONObject job) {
        return db2Json(job);
    }

    /**
     * Get the uuid for the given App context
     *
     * @param context the context
     * @return the UUID, null if not available
     */
    public static String getUUID(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("uuid", null);
    }

    /**
     * Get a readable db instance
     *
     * @return a readable db instance
     */
    public static SQLiteDatabase getReadableDb() {
        if (energyMonitorDbHelper != null) {
            return energyMonitorDbHelper.getReadableDatabase();
        }
        return null;
    }

    /**
     * Return the result of the last upload.
     *
     * @param serverUploadResult result returned by the server
     */
    public void setServerUploadResult(JSONObject serverUploadResult) {
        // We store the data into the preferences. Maybe store statistics into DB as well?

        Calendar c = Calendar.getInstance();

        SharedPreferences.Editor prefedit = preferences.edit();
        prefedit.putLong("LastSuccessfulUpload_Time", c.getTimeInMillis());
        Iterator<?> keys = serverUploadResult.keys();
        int totalChangedItems = 0;
        // Get amount of changed items from all returned fields.
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                if (serverUploadResult.getInt(key) > 0) {
                    totalChangedItems += serverUploadResult.getInt(key);
                }
            } catch (JSONException e) {
                Log.d("TAG", "Not an int key: " + key);
            }
        }
        prefedit.putInt("LastSuccessfulUpload_Changed", totalChangedItems);
        prefedit.apply();
    }

    /**
     * Get the timestamp of the last data upload
     *
     * @param context The application context
     * @return The timestamp, -1 if not available
     */
    public static long getLastServerUploadTimestamp(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getAll().get("LastSuccessfulUpload_Time") instanceof Long) {
            return prefs.getLong("LastSuccessfulUpload_Time", -1);
        }
        return -1;
    }

    /**
     * Set the last upload code to the preferences
     *
     * @param status The status code
     */
    public void setLastServerUploadStatuscode(int status) {
        preferences.edit().putInt("LastSuccessfulUpload_Code", status).apply();
        Intent intent = new Intent(UPLOAD_DONE_BROADCAST_ACTION);
        sendBroadcast(intent);
    }

    /**
     * Get the last status update code from the preferences
     *
     * @return The status code
     */
    public int getLastServerUploadStatuscode() {
        return preferences.getInt("LastSuccessfulUpload_Code", -1);
    }

    /**
     * Get the last status code as plain text
     *
     * @return The string representing the last status
     */
    public String getLastServerUploadStatusText() {
        switch (getLastServerUploadStatuscode()) {
            case ServerCommunicationHandler.CHECK_URL:
                return getString(R.string.export_server_check_url_failed);
            case ServerCommunicationHandler.CHECK_NETWORK:
                return getString(R.string.export_server_check_network_failed);
            case ServerCommunicationHandler.REQUEST_TOKEN:
                return getString(R.string.export_server_request_token_failed);
            case ServerCommunicationHandler.REQUEST_RANGE:
                return getString(R.string.export_server_check_existing_datasets_failed);
            case ServerCommunicationHandler.EXPORT_DB:
                return getString(R.string.export_server_export_data_failed);
            case ServerCommunicationHandler.UPLOAD_DB:
                return getString(R.string.export_server_upload_data_failed);
            case ServerCommunicationHandler.DONE:
                return getString(R.string.export_server_done);
            default:
                return "-";
        }
    }

    /**
     * Get the time of the last successful upload
     *
     * @return String with the time or null if never uploaded data
     */
    public String getLastServerUploadTime() {
        //return preferences.getString("LastSuccessfulUpload_Time", null);

        long timestamp = -1;

        // Fallback for old version where the time was stored as a string
        if (preferences.getAll().get("LastSuccessfulUpload_Time") instanceof String) {
            return preferences.getString("LastSuccessfulUpload_Time", getString(R.string.export_time_never));
        }

        timestamp = preferences.getLong("LastSuccessfulUpload_Time", -1);

        if (timestamp < 0) {
            return getString(R.string.export_time_never);
        }

        return DateUtils.formatDateTime(
                getApplicationContext(),
                timestamp,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
        );
    }

    /**
     * Get the number changed item at the server side during the last upload. -1 if no values are available.
     *
     * @return number of changed items or -1 if no values are available.
     */
    public int getLastServerUploadItems() {
        return preferences.getInt("LastSuccessfulUpload_Changed", -1);
    }

    /**
     * Called if a dataset was succesfully stored by a broadcast receiver
     */
    public void setDatasetStored() {

        Calendar c = Calendar.getInstance();
        lastDataItemStored = DateUtils.formatDateTime(
                getApplicationContext(),
                c.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
        );
    }

    /**
     * Returns the timestamp when the last data item was stored
     *
     * @return String with the timestamp of null if no value is available
     */
    public String getLastDataItemStored() {
        return lastDataItemStored;
    }

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

        // Job Creator
        // Todo: rm if using different job scheduler like android worker
        JobManager.create(this).addJobCreator(new UploadJobCreator());
        // We cancel all existing work as it might not be done before
        Log.d(TAG, "Cancelled " + JobManager.instance().cancelAll() + " jobs from the JobManager");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

/*
        // Ensure that the notification is again activated if doze is there and app not in exception list
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())){
                // We have doze and User is not in exceptions...
                preferences.edit().putBoolean("show_notification_bar", true).apply();
            }
        }
*/

        // Required for Android > O
        createNotificationChannel();

        if (showNotificationBar()) {
            mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            updateNotification();
        }

        if (energyMonitorDbHelper == null) {
            energyMonitorDbHelper = new EnergyMonitorDbHelper(getApplicationContext());
        }
        writableDb = energyMonitorDbHelper.getWritableDatabase();
        Log.d(TAG, "db path: " + writableDb.getPath());

        powerBroadcastReceiver = new PowerBroadcastReceiver(this, writableDb);
        screenBroadcastReceiver = new ScreenBroadcastReceiver(this, writableDb);
        wiFiBroadcastReceiver = new WiFiBroadcastReceiver(this, writableDb);
        airplaneModeBroadcastReceiver = new AirplaneModeBroadcastReceiver(this, writableDb);
        byteCountBroadcastReceiver = new ByteCountBroadcastReceiver(this, writableDb);
        bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(this, writableDb);
        cellularBroadcastReceiver = new CellularBroadcastReceiver(this, writableDb);

        dataUploadBroadcastReceiver = new DataUploadBroadcastReceiver(this, writableDb);

        boolean dataCollectionEnabled = preferences.getBoolean("data_collection_enabled", true);
        if (dataCollectionEnabled) {
            startDataCollection();
        } else {
            // Should not be started but ensure all notifications are correct
            stopDataCollection();
        }


        dataUploadBroadcastReceiver.register(this);

        boolean automaticDataUploadEnabled = preferences.getBoolean("automatic_data_upload", true);

        // Schedule upload job?

        WorkManager workManager = WorkManager.getInstance();

        if (workManager != null) {
            // Cancel all work (if existent). We use the evernote job library
            workManager.cancelAllWork();
        }

        if (automaticDataUploadEnabled) {
            Log.d(TAG, "Scheduling upload job");
            JobUploadWorker.scheduleJob(this);
        }

        /*
        // WorkManager part

        WorkManager workManager = WorkManager.getInstance();
        if (workManager != null){

            // TODO: Check
            workManager.cancelAllWork();

            if (automaticDataUploadEnabled && numUploadJobActive() == 0){

                //workManager.enqueue(UploadWorker.getWorkRequest(this));
                workManager.enqueueUniquePeriodicWork(UploadWorker.TAG, ExistingPeriodicWorkPolicy.KEEP, UploadWorker.getWorkRequest(this));
                Log.d(TAG, "Upload worker started");
            } else {
                Log.d(TAG, "No upload job scheduled");
            }
        } else {
            Log.e(TAG, "Cannot get work manager instance");
        }

        */

    }

    /**
     * Got start command
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }


    /**
     * Stop service
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        stopDataCollection();
        if (energyMonitorDbHelper != null) {
            energyMonitorDbHelper.close();
        }
        if (mNM != null) {
            mNM.cancelAll();
        }
        //restartService();

        // Cancel upload jobs (if existent)
        JobUploadWorker.cancelJob();

        /*

        WorkManager workManager = WorkManager.getInstance();
        if (workManager != null){
            workManager.cancelUniqueWork(UploadWorker.TAG);
            //workManager.cancelAllWorkByTag(UploadWorker.TAG);
        }
        */

        dataUploadBroadcastReceiver.unregister(this);

    }


    /**
     * Return the binder reference
     *
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
     *
     * @param text The notification string
     */
    private void showNotification(String text) {

        if (mNM == null) {
            return;
        }
        String lastTime = getLastServerUploadTime();
        String lastExport = getString(R.string.export_last_upload, lastTime, getLastServerUploadStatusText());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class)
                , 0);

        /*
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.smallicon)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(text)
                .setContentText(lastExport)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();
        */
        Notification notification = new NotificationCompat.Builder(this, TAG + ".notification_channel")
                .setSmallIcon(R.mipmap.smallicon)
                .setContentTitle(text)
                .setContentText(lastExport)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //.setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setTicker(text)
                .setContentIntent(contentIntent)
                .build();

        mNM.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Update the notification bar with the most recent message
     */
    public void updateNotification() {
        if (preferences.getBoolean("data_collection_enabled", true)) {
            showNotification(getString(R.string.DataCollectionRunning));
        } else {
            showNotification(getString(R.string.DataCollectionStopped));
        }
    }


    /**
     * Handle updates from the UI
     */
    public void updatedSetting(String key) {
        if (key.equals("data_collection_enabled")) {
            boolean dataCollectionEnabled = preferences.getBoolean("data_collection_enabled", true);
            if (dataCollectionEnabled && !dataCollectionRunning) {
                startDataCollection();
            } else if (!dataCollectionEnabled && dataCollectionRunning) {
                stopDataCollection();
            }
        } else if (key.equals("show_notification_bar")) {
            //if(!Helper.isPowerSaving(this)){
            boolean showNotificationBar = showNotificationBar();
            if (showNotificationBar && mNM == null) {
                Log.d(TAG, "Enable Notification Bar");
                mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                updateNotification();
            } else if (!showNotificationBar && mNM != null) {
                Log.d(TAG, "Disable Notification Bar");
                mNM.cancelAll();
                mNM = null;
                updateNotification();
            }
            //} else {
            //    if (!preferences.getBoolean("show_notification_bar", true))
            //    Toast.makeText(this, getText(R.string.cannot_deactivate_icon), Toast.LENGTH_LONG).show();
            //    preferences.edit().putBoolean("show_notification_bar", true).apply();
            //}
            updateNotification();

        } else if (key.equals("automatic_data_upload")) {

            if (!JobUploadWorker.isJobRunning() && preferences.getBoolean("automatic_data_upload", true)) {
                Log.d(TAG, "Job not running. Starting...");
                JobUploadWorker.scheduleJob(this);
            } else if (JobUploadWorker.isJobRunning() && !preferences.getBoolean("automatic_data_upload", true)) {
                Log.d(TAG, "Job is running. Stopping...");
                JobUploadWorker.cancelJob();
            }
        } else if (
                key.equals("automatic_data_upload_only_on_unmetered_connection") &&
                        JobUploadWorker.isJobRunning() &&
                        preferences.getBoolean("automatic_data_upload_only_on_unmetered_connection", true) != JobUploadWorker.onlyUnmeteredConnection()
                ) {
            Log.d(TAG, "Setting for unmetered connection changed. Restarting job");
            JobUploadWorker.scheduleJob(this);
        } else if (
                key.equals("automatic_data_upload_interval") &&
                        JobUploadWorker.isJobRunning() &&
                        JobUploadWorker.hasUploadStateChanged(this)
                ) {
            Log.d(TAG, "Upload interval changed. Restarting job");
            JobUploadWorker.scheduleJob(this);
        }
    }

    /**
     * Checks whether the app is showing a notification or not
     *
     * @return true if notification is enabled
     */
    public boolean isNotificationEnabled() {
        return mNM != null;
    }

    /**
     * Return a string with some DB statistics
     *
     * @return Get a string containing the statistics
     */
    String getDbStats() {
        return EnergyMonitorDbHelper.getDbStatistics(writableDb);
    }


    /**
     * Get some battery discharge characteristics.
     *
     * @param minTime Minimum time in milliseconds
     * @param maxTime Maximum time in milliseconds
     * @return List of Battery objects
     */
    public List<BatteryChangeObject> getBatteryStats(long minTime, long maxTime) {
        return EnergyMonitorDbHelper.getDischargeBehaviour(writableDb, minTime, maxTime);
    }

    /**
     * Exports the complete database to a json object.
     *
     * @return JSONObject database as json
     */
    public JSONObject db2Json(JSONObject exportRange) {
        if (exportRange == null) {
            Log.d(TAG, "No range requested");
        } else {
            Log.d(TAG, "Range: " + exportRange);
        }
        JSONObject jsonObject = new JSONObject();

        // Battery
        Cursor batteryStatusCursor = writableDb.query(
                EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME),
                null,
                null,
                null,
                null);
        Cursor byteCountCursor = writableDb.query(
                EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME),
                null,
                null,
                null,
                null);
        Cursor bluetoothCursor = writableDb.query(
                EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME),
                null,
                null,
                null,
                null);
        Cursor flightModeCursor = writableDb.query(
                EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME),
                null,
                null,
                null,
                null);
        Cursor wifiCursor = writableDb.query(
                EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME),
                null,
                null,
                null,
                null);
        Cursor screenStatusCursor = writableDb.query(
                EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME),
                null,
                null,
                null,
                null);
        Cursor cellularStatusCursor = writableDb.query(
                EnergyMonitorContract.CellularStatusEntry.TABLE_NAME,
                null,
                EnergyMonitorDbHelper.getWhereForQuery(exportRange, EnergyMonitorContract.CellularStatusEntry.TABLE_NAME),
                null,
                null,
                null,
                null);

        try {
            jsonObject.put(EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(batteryStatusCursor));
            jsonObject.put(EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(byteCountCursor));
            jsonObject.put(EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(bluetoothCursor));
            jsonObject.put(EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(flightModeCursor));
            jsonObject.put(EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(wifiCursor));
            jsonObject.put(EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(screenStatusCursor));
            jsonObject.put(EnergyMonitorContract.CellularStatusEntry.TABLE_NAME, EnergyMonitorDbHelper.cursorToJson(cellularStatusCursor));

            String uuid = preferences.getString("uuid", "None");
            String timezone = TimeZone.getDefault().getID();
            boolean hasBle = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
            long exportTimestamp = System.currentTimeMillis();

            jsonObject.put("UUID", uuid);
            jsonObject.put("TIMEZONE", timezone);
            jsonObject.put("HAS_BLE", hasBle);
            jsonObject.put("EXPORT_TIMESTAMP", exportTimestamp);
            jsonObject.put("VERSION_CODE", BuildConfig.VERSION_CODE);

            Log.d(TAG, "UUID: " + uuid + " timezone: " + timezone + " export timestamp: " + exportTimestamp);


        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Json object size: " + jsonObject.toString().length());

        return jsonObject;
    }

    public void exportDbToServer() {
        Log.d(TAG, "Export db to server");
    }

    /**
     * Export the db json object
     *
     * @return A file handler to the created file
     */
    public File exportData() {

        JSONObject jsonObject = db2Json(null);

        try {
            File f = new File(getFilesDir(),

                    Long.toString(System.currentTimeMillis() / 1000) +
                            "_" +
                            getString(R.string.export_filename_prefix) +
                            "_" +
                            preferences.getString("uuid", "None") +
                            ".json.gz"
            );
            FileOutputStream fos = openFileOutput(f.getName(), Context.MODE_PRIVATE);
            GZIPOutputStream gos = new GZIPOutputStream(fos);
            gos.write(jsonObject.toString().getBytes());
            gos.close();
            fos.close();

            return f;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Register the broadcast receiver to start the data collection
     */
    public void startDataCollection() {
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
    public void stopDataCollection() {
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
    private void restartService() {
        Log.d(TAG, "Restart intent");
        Toast.makeText(this, "Sending restart intend", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("de.uni_bremen.comnets.resourcemonitor.restart");
        sendBroadcast(intent);
    }

    /**
     * Upload the data to the server
     *
     * @param context The context
     */
    public void uploadToServer(Context context) {

        // TODO: Last upload status
        if (context == null) {
            // Background upload
            try {
                long lastUpload = getLastServerUploadTimestamp(getApplicationContext());
                if (System.currentTimeMillis() < lastUpload + JobUploadWorker.getMinUploadIntervalLimit(this) * 1000) {
                    Log.d(TAG, "Just uploaded data. Skipping this upload");
                    return;
                }
                new ExportDatabaseToServerTask(getApplicationContext(), this, true).execute();
                Log.d(TAG, "Background Upload done");
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: Handle, fix and report exceptions of the background upload
            }

        } else {
            new ExportDatabaseToServerTask(context, this, false).execute();
            Log.d(TAG, "Foreground Upload done");
        }
        updateNotification();

    }

    /**
     * Upload the data to the server without UI interaction
     */
    public void uploadToServer() {
        uploadToServer(null);
    }

    /**
     * Creates a notification channel for Android API 26+
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(TAG + ".notification_channel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Return if a notification bar should be shown
     *
     * @return true if it should be shown, otherwise false
     */
    private boolean showNotificationBar() {
        // TODO change for API level 26 and higher!
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true;
        }
        */
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_notification_bar", true);
    }

    /**
     * Check for active unique background jobs
     *
     * @return True if a background job is active, otherwise false
     */
/*
workManager stuff
    private int numUploadJobActive() {
        WorkManager workManager = WorkManager.getInstance();
        if (workManager == null) {
            Log.e(TAG, "WorkManager: Cannot get instance of work manager!");
            return 0;
        }

        //List<WorkStatus> statuses = workManager.getStatusesByTag(UploadWorker.TAG).getValue();
        List<WorkStatus> statuses = workManager.getStatusesForUniqueWork(UploadWorker.TAG).getValue();
        if (statuses == null){
            Log.d(TAG, "WorkManager: statuses null");
            return 0;
        }
        Log.d(TAG, "WorkManager: Status size: " + statuses.size());
        return statuses.size();

    }

 */

  /*
  workManager stuff
    private void cancelAllUploadJobs(){
        WorkManager workManager = WorkManager.getInstance();
        if (workManager == null){
            Log.e(TAG, "WorkManager: Cannot get instance of work manager!");
            return;
        }

        workManager.cancelUniqueWork(UploadWorker.TAG);

        List<WorkStatus> statuses = workManager.getStatusesByTag(UploadWorker.TAG).getValue();
        if (statuses == null){
            Log.d(TAG, "WorkManager: status: null");
            return;
        }

        for (WorkStatus status : statuses){
            Log.d(TAG, "WorkManager: Cancelling job");
            workManager.cancelWorkById(status.getId());
        }
    }
    */
}
