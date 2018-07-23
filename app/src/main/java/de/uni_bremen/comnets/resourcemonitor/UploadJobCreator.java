package de.uni_bremen.comnets.resourcemonitor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class UploadJobCreator implements JobCreator {


    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        if (tag.equals(JobUploadWorker.TAG)){
            return new JobUploadWorker();
        } else {
            return null;
        }
    }
}
