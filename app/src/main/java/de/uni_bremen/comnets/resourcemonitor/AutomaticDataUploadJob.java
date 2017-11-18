package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver.DataUploadBroadcastReceiver;

/**
 * Class for the automatic data upload. Based on firebase as
 *
 * - https://github.com/evernote/android-job is not working properly on the current Android version
 * - JobScheduler is only available since API level 21 (M)
 *
 * Send a broadcast to the MonitorService which triggers the upload itself
 */

public class AutomaticDataUploadJob extends JobService {
    public static final String TAG = AutomaticDataUploadJob.class.getSimpleName();

    // Tag for identifying the job to for example cancel it later
    private static final String UPLOAD_JOB_TAG = TAG+".uploadJob";

    // The dispatcher instance
    private FirebaseJobDispatcher jobDispatcher = null;


    /**
     * Is the Job active, i.e., started?
     * @return  True, if running
     */
    public boolean isStarted(){
        return jobDispatcher != null;
    }

    /**
     * Start the job
     *
     * @param context   The application context
     */
    public void start(Context context){
        Log.d(TAG, "Start upload job");
        if (jobDispatcher == null){
            jobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
            Job uploadJob = AutomaticDataUploadJob.buildJob(jobDispatcher, UPLOAD_JOB_TAG, context);
            jobDispatcher.mustSchedule(uploadJob);
        } else {
            Log.d(TAG, "Already running");
        }


    }

    /**
     * Stop the job
     */
    public void stop(){
        Log.d(TAG, "Stop upload job");
        if (jobDispatcher != null) {
            jobDispatcher.cancel(UPLOAD_JOB_TAG);
            jobDispatcher = null;
            Log.d(TAG, "Stopped");
        }

    }

    /**
     * Triggered by the dispatcher if the job is started
     *
     * @param params    The parameters
     * @return  True if no work is going on, false if the triggered thread is still active
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        // Check if sufficient time passed by (upload if the last upload occurred more than MIN_AUTO_UPLOAD_INTERVAL seconds ago)
        if (System.currentTimeMillis() > MonitorService.getLastServerUploadTimestamp(getApplicationContext()) + MonitorService.MIN_AUTO_UPLOAD_INTERVAL * 1000) {

            // Send upload trigger

            Intent intent = new Intent();
            intent.setAction(DataUploadBroadcastReceiver.DATA_UPLOAD);
            sendBroadcast(intent);
            Log.d(TAG, "Sent upload intent");
        } else {
            Log.d(TAG, "Skipped current upload");
        }

        return false; // Answer to question: Is still work going on?
    }

    /**
     * Called if a job is cancelled
     *
     * @param params    The parameters
     * @return          True if the job should be retrieved, false otherwise
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "OnStopJob: Job Cancelled");
        return false; // Answers the question: "Should this job be retried?"
    }

    /**
     * Returns an initialized job from the the JobBuilder.
     *
     * @param dispatcher    The dispatcher
     * @param tag           The tag of the newly generated job
     * @param context       The context
     * @return              The new job
     */
    static Job buildJob(FirebaseJobDispatcher dispatcher, String tag, Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useUnmeteredConnection = preferences.getBoolean("automatic_data_upload_only_on_unmetered_connection", true);

        Job.Builder builder = dispatcher.newJobBuilder()
                .setService(AutomaticDataUploadJob.class)
                .setTag(tag)
                .setReplaceCurrent(true)
                .setRecurring(true)
                .setTrigger(
                        Trigger.executionWindow(
                                MonitorService.MIN_PERIOD_DATA_UPLOAD_INTERVAL,
                                MonitorService.MAX_PERIOD_DATA_UPLOAD_INTERVAL
                        )
                )
                //.setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR);

        if (useUnmeteredConnection) {
            builder.setConstraints(
                    //Constraint.DEVICE_CHARGING,
                    Constraint.ON_UNMETERED_NETWORK
                    //Constraint.DEVICE_IDLE
            );
            Log.d(TAG, "Upload on unmetered network");
        } else {
            Log.d(TAG, "Upload on metered network");
        }

        return builder.build();
    }
}
