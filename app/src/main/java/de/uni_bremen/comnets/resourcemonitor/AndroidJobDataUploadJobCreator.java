package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/*
 * Android job creator based on the evernote Android-Job library, https://github.com/evernote/android-job
 */

public class AndroidJobDataUploadJobCreator implements JobCreator {

    public static final String TAG = AndroidJobDataUploadJobCreator.class.getSimpleName();

    Context context = null;
    AndroidJobDataUploadJobCreator(Context context){
        this.context = context;
    }

    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        if (AndroidJobDataUploadJob.TAG.equals(tag)) {
            return new AndroidJobDataUploadJob(this.context);
        } else {
            return null;
        }
    }
}
