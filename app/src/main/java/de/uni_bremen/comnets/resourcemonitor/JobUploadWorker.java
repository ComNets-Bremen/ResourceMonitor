package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.DataUploadBroadcastReceiver;

/**
 * Job class for the evernote android-job library
 */
public class JobUploadWorker extends Job {

    public static final String TAG = JobUploadWorker.class.getSimpleName();

    static private boolean useUnmeteredConnection = true;

    static private int jobid = -1;

    static private String lastStartIntervalId = null;

    // Settings for the background upload intervals
    public static final int DEFAULT_MIN_DATA_UPLOAD_INTERVAL_LIMIT = 60 * 60;     // (in seconds) At maximum once per hour (externally triggered) for the upload
    public static final int DEFAULT_MIN_PERIOD_DATA_UPLOAD_INTERVAL = 60 * 60 * 24;  // (in seconds) min time
    public static final int DEFAULT_MAX_PERIOD_DATA_UPLOAD_INTERVAL = 60 * 60 * 48;  // (in seconds) max time


    @Override
    @NonNull
    protected Result onRunJob(Params params) {

        Log.d(TAG, "onRunJob uploadWorker");
        if (System.currentTimeMillis() > MonitorService.getLastServerUploadTimestamp(getContext()) + TimeUnit.SECONDS.toMillis(getMinUploadInterval(getContext()))) {
            Intent intent = new Intent();
            intent.setAction(DataUploadBroadcastReceiver.DATA_UPLOAD);
            getContext().sendBroadcast(intent);
            Log.d(TAG, "Sent upload intent");

        } else {
            Log.d(TAG, "Skipped current upload");
        }        return Result.SUCCESS;
    }


    /**
     * Schedule a new job. Existing jobs are cancelled
     *
     * @param context   The application context
     * @return          The job id
     */
    public static int scheduleJob(Context context) {
        Log.d(TAG, "schedule Job");

        if (jobid > 0) {
            Log.d(TAG, "In schedule job: cancel job");
            cancelJob();
        }


        JobRequest.Builder builder = new JobRequest.Builder(TAG);
        long interval = TimeUnit.SECONDS.toMillis(getMaxUploadInterval(context));
        long flex = TimeUnit.SECONDS.toMillis(getMaxUploadInterval(context) - getMinUploadInterval(context));
        Log.d(TAG, "Adding job with the following data: Interval: " + interval +  " flex: " + flex);
        builder.setPeriodic(interval, flex);
        builder.setRequiresBatteryNotLow(true);
        builder.setRequirementsEnforced(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        useUnmeteredConnection = preferences.getBoolean("automatic_data_upload_only_on_unmetered_connection", true);

        if (useUnmeteredConnection) {
            builder.setRequiredNetworkType(JobRequest.NetworkType.UNMETERED);
        }


        lastStartIntervalId = PreferenceManager.getDefaultSharedPreferences(context).getString("automatic_data_upload_interval", null);

        jobid = builder.build().schedule();

        Log.d(TAG, "Created new job with id " + jobid);

        return jobid;

    }

    /**
     * Cancel an existing job
     */
    public static void cancelJob() {
        Log.d(TAG, "In Cancel Job");
        JobManager.instance().cancel(jobid);
        jobid = -1;
    }

    /**
     * Check if a job is running
     *
     * @return  True if job is running
     */
    public static boolean isJobRunning() {
        return jobid > 0;
    }


    /**
     * True if the  upload is only performed using an unmetered connection
     *
     * @return true if unmetered, otherwise false
     */
    public static boolean onlyUnmeteredConnection() {
        return useUnmeteredConnection;
    }


    /**
     * Get the minimum upload interval
     *
     * @param context The context
     * @return The interval in seconds
     */
    public static int getMinUploadInterval(Context context){
        String pref = PreferenceManager.getDefaultSharedPreferences(context).getString("automatic_data_upload_interval", null);
        if (pref == null) return DEFAULT_MIN_PERIOD_DATA_UPLOAD_INTERVAL;
        Log.d(TAG, "interval ID: " + pref);

        switch (pref){
            case "1":
                // 1h
                return 1*60*60;
            case "2":
                // 12h
                return 12*60*60;
            case "3":
                // 24h
                return 24*60*60;
            case "4":
                // 36h
                return 36*60*60;
            case "5":
                // 3d
                return 3*24*60*60;
            case "6":
                // 4d
                return 4*24*60*60;
            default:
                Log.e(TAG, "Undefined type for min time interval: " + pref);
        }
        return DEFAULT_MIN_PERIOD_DATA_UPLOAD_INTERVAL;

    }

    /**
     * Get the maximum upload interval
     *
     * @param context The context
     * @return The interval in seconds
     */
    public static int getMaxUploadInterval(Context context){
        String pref = PreferenceManager.getDefaultSharedPreferences(context).getString("automatic_data_upload_interval", null);
        if (pref == null) return DEFAULT_MAX_PERIOD_DATA_UPLOAD_INTERVAL;

        Log.d(TAG, "interval ID: " + pref);

        switch (pref){
            case "1":
                // 2h
                return 2*60*60;
            case "2":
                // 24h
                return 24*60*60;
            case "3":
                // 36h
                return 36*60*60;
            case "4":
                // 72h
                return 72*60*60;
            case "5":
                // 4d
                return 4*24*60*60;
            case "6":
                // 7d
                return 7*24*60*60;
            default:
                Log.e(TAG, "Undefined type for max time interval: " + pref);
        }

        return DEFAULT_MAX_PERIOD_DATA_UPLOAD_INTERVAL;
    }

    /**
     * Return the absolute minimum time limit between two uploads
     *
     * @param context   The context
     * @return  The interval in seconds
     */
    public static int getMinUploadIntervalLimit(Context context){
        return DEFAULT_MIN_DATA_UPLOAD_INTERVAL_LIMIT;
    }

    /**
     * Get the id of the last used start interval identifier
     *
     * @return The identifier
     */
    public static String getLastStartIntervalId() {
        return lastStartIntervalId;
    }

    public static boolean hasUploadStateChanged(Context context){
        String pref = PreferenceManager.getDefaultSharedPreferences(context).getString("automatic_data_upload_interval", null);
        if (pref == null) return true;
        return !pref.equals(getLastStartIntervalId());
    }

}
