package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.DataUploadBroadcastReceiver;

public class UploadWorker extends Worker {

    public static final String TAG = UploadWorker.class.getSimpleName();


    private Context context = null;

    public UploadWorker(Context context){
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (System.currentTimeMillis() > MonitorService.getLastServerUploadTimestamp(this.context) + MonitorService.MIN_AUTO_UPLOAD_INTERVAL * 1000) {
        Intent intent = new Intent();
        intent.setAction(DataUploadBroadcastReceiver.DATA_UPLOAD);
        this.context.sendBroadcast(intent);
        Log.d(TAG, "Sent upload intent");
    } else {
        Log.d(TAG, "Skipped current upload");
    }
        return Result.SUCCESS;
    }


    /**
     * Return the contraints for the worker
     *
     * @param context   The application context
     * @return          The constaints
     */
    public static Constraints getWorkerConstaints(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useUnmeteredConnection = preferences.getBoolean("automatic_data_upload_only_on_unmetered_connection", true);
        if (useUnmeteredConnection) {
            return new Constraints.Builder().setRequiresBatteryNotLow(true).setRequiredNetworkType(NetworkType.UNMETERED).build();
        }else {
            return new Constraints.Builder().setRequiresBatteryNotLow(true).build();
        }
    }

    public static PeriodicWorkRequest getWorkRequest(Context context){
        return new PeriodicWorkRequest.Builder(UploadWorker.class, MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL, TimeUnit.SECONDS, MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL-MonitorService.MIN_DATA_UPLOAD_INTERVAL_LIMIT, TimeUnit.SECONDS).setConstraints(getWorkerConstaints(context)).build();
    }
}
