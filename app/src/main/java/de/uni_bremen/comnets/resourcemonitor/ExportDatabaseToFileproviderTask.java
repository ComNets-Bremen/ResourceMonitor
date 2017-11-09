package de.uni_bremen.comnets.resourcemonitor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Class to export the database using an Android intent via mail or file sharing service
 */

public class ExportDatabaseToFileproviderTask extends AsyncTask<Void, Void, Uri> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Intent m_intent;
    private String m_dialogTitle;
    private WeakReference<Context> m_context = null;
    private WeakReference<Activity> m_activity = null;
    private WeakReference<MonitorService> m_monitorService = null;
    private ProgressDialog progressDialog = null;

    ExportDatabaseToFileproviderTask(Context context, Activity activity, MonitorService monitorService, Intent intent, String dialogTitle){
        super();
        m_context = new WeakReference<Context>(context);
        m_monitorService = new WeakReference<MonitorService>(monitorService);
        m_activity = new WeakReference<Activity>(activity);
        m_intent = intent;
        m_dialogTitle = dialogTitle;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (m_context != null && m_activity != null) {
            progressDialog = ProgressDialog.show(m_activity.get(), m_context.get().getString(R.string.export_progress_title), m_context.get().getString(R.string.export_progress_text), false);
        }
    }

    @Override
    protected Uri doInBackground(Void... params) {
        if (m_monitorService != null && m_context != null) {
            File f = m_monitorService.get().exportData();
            return FileProvider.getUriForFile(m_context.get(), BuildConfig.APPLICATION_ID + ".fileprovider", f);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Uri uri) {
        super.onPostExecute(uri);
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        Log.d(TAG, "URI: " + uri);
        if (uri != null && m_context != null && m_intent != null){
            m_intent.putExtra(Intent.EXTRA_STREAM, uri);
            m_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            m_context.get().startActivity(Intent.createChooser(m_intent, m_dialogTitle));
        } else {
            Log.e(TAG, "Error creating the export dialog");
        }

        recycle();
    }

    /**
     * Tidy up the weak references
     */
    private void recycle(){
        if (m_context != null){
            m_context.clear();
            m_context = null;
        }

        if (m_activity != null){
            m_activity.clear();
            m_activity = null;
        }

        if (m_monitorService != null){
            m_monitorService.clear();
            m_monitorService = null;
        }
    }
}
