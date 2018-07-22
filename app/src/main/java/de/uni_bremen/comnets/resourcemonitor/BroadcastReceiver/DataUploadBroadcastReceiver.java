package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.uni_bremen.comnets.resourcemonitor.MonitorService;

/**
 * BroadcastReceiver for the data upload.
 * Triggered by the UploadWorker
 */
public class DataUploadBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    private static final String TAG = DataUploadBroadcastReceiver.class.getSimpleName();


    // The action for the intent filter
    public static final String DATA_UPLOAD = DataUploadBroadcastReceiver.class.getCanonicalName() + ".ACTION";

    public DataUploadBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db){
        super(monitorService, db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent, starting upload job");
        // Trigger the upload using the MonitorService
        getMonitorServiceInstance().uploadToServer();
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(DATA_UPLOAD);

        return intentFilter;
    }

    @Override
    public BroadcastReceiverDescriptor getReceiverDescription() {
        // No user data collected -> not mentioned for the GDPR
        return null;
    }
}
