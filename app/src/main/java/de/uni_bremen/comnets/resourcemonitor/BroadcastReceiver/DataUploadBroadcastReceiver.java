package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import de.uni_bremen.comnets.resourcemonitor.MonitorService;

/**
 * BroadcastReceiver for the data upload.
 * Triggered by the AutomaticDataUploadJob
 */
public class DataUploadBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    // The action for the intent filter
    public static final String DATA_UPLOAD = DataUploadBroadcastReceiver.class.getCanonicalName() + ".ACTION";

    public DataUploadBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db){
        super(monitorService, db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Trigger the upload using the MonitorService
        getMonitorServiceInstance().uploadToServer();
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(DATA_UPLOAD);

        return intentFilter;
    }
}
