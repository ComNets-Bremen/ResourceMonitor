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

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.DataUploadBroadcastReceiver;

/**
 * Job class for the evernote android-job library
 */
public class JobUploadWorker extends Job {

    public static final String TAG = JobUploadWorker.class.getSimpleName();

    static private boolean useUnmeteredConnection = true;

    static private int jobid = -1;


    @Override
    @NonNull
    protected Result onRunJob(Params params) {

        Log.d(TAG, "onRunJob uploadWorker");
        if (System.currentTimeMillis() > MonitorService.getLastServerUploadTimestamp(getContext()) + MonitorService.MIN_PERIOD_DATA_UPLOAD_INTERVAL * 1000) {
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
        builder.setPeriodic(TimeUnit.SECONDS.toMillis(MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL), TimeUnit.SECONDS.toMillis(MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL - MonitorService.MIN_DATA_UPLOAD_INTERVAL_LIMIT));
        builder.setRequiresBatteryNotLow(true);
        builder.setRequirementsEnforced(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        useUnmeteredConnection = preferences.getBoolean("automatic_data_upload_only_on_unmetered_connection", true);

        if (useUnmeteredConnection) {
            builder.setRequiredNetworkType(JobRequest.NetworkType.UNMETERED);
        }

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
}
