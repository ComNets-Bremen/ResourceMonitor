package de.uni_bremen.comnets.resourcemonitor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;

/**
 * Class to export the database using an Android intent via mail or file sharing service
 */

public class ExportDatabaseTask extends AsyncTask<Void, Void, Uri> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Intent m_intent;
    private String m_dialogTitle;
    private Context m_context;
    private Activity m_activity;
    private MonitorService m_monitorService;
    private ProgressDialog progressDialog;

    ExportDatabaseTask(Context context, Activity activity, MonitorService monitorService, Intent intent, String dialogTitle){
        super();
        m_context = context;
        m_monitorService = monitorService;
        m_activity = activity;
        m_intent = intent;
        m_dialogTitle = dialogTitle;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(m_activity, m_context.getString(R.string.export_progress_title), m_context.getString(R.string.export_progress_text), false);
    }

    @Override
    protected Uri doInBackground(Void... params) {
        File f = m_monitorService.exportData();
        return  FileProvider.getUriForFile(m_context, BuildConfig.APPLICATION_ID + ".fileprovider", f);
    }

    @Override
    protected void onPostExecute(Uri uri) {
        super.onPostExecute(uri);
        progressDialog.dismiss();
        Log.d(TAG, "URI: " + uri);
        m_intent.putExtra(Intent.EXTRA_STREAM, uri);
        m_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        m_context.startActivity(Intent.createChooser(m_intent, m_dialogTitle));
    }
}
