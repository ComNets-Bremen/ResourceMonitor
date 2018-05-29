package de.uni_bremen.comnets.resourcemonitor;

/*
 * Android job based on the evernote Android-Job library, https://github.com/evernote/android-job
 */

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

public class AndroidJobDataUploadJob extends Job {
    public static final String TAG = AndroidJobDataUploadJob.class.getSimpleName();

    private Context context = null;

    public AndroidJobDataUploadJob(Context context){
        this.context = context;
    }

    /**
     * Executed by the AndroidJob library
     *
     * @param params
     * @return  The job result
     */
    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        if (System.currentTimeMillis() > MonitorService.getLastServerUploadTimestamp(this.context) + MonitorService.MIN_AUTO_UPLOAD_INTERVAL * 1000) {

            // Send upload trigger

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
     * Schedule a job. Return the id of the new job
     *
     * @param context
     * @return  The job id
     */
    public static int scheduleJob(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useUnmeteredConnection = preferences.getBoolean("automatic_data_upload_only_on_unmetered_connection", true);

        JobRequest.Builder jb = new JobRequest.Builder(TAG);
        jb.setPeriodic(
                TimeUnit.SECONDS.toMillis(MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL),
                TimeUnit.SECONDS.toMillis(MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL - MonitorService.MIN_PERIOD_DATA_UPLOAD_INTERVAL)
        );
        if (useUnmeteredConnection) {
            Log.d(TAG, "Unmetered connection only");
            jb.setRequiredNetworkType(JobRequest.NetworkType.UNMETERED);
        }

        return jb.build().schedule();
    }

    /**
     * Cancel a job with a given job id
     *
     * @param jobid
     */
    public static void cancelJob(int jobid){
        JobManager.instance().cancel(jobid);
    }


}
