package de.uni_bremen.comnets.resourcemonitor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Class to upload data to the server
 */
class ExportDatabaseToServerTask extends AsyncTask<Void, ExportDatabaseToServerTask.ExportDatabaseStatus, ExportDatabaseToServerTask.ExportDatabaseStatus> {

    // Status for a) update the progress dialog and b) return status messages
    enum ExportDatabaseStatus {CHECK_URL, CHECK_NETWORK, REQUEST_TOKEN, REQUEST_RANGE, EXPORT_DB, UPLOAD_DB, DONE}

    private static final String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog progressDialog;
    private Context m_context;
    private MonitorService m_service;
    private Activity m_activity;

    /* TODO
     * - nicer status ids
     * - ...
     */

    /**
     * Default constructor
     *
     * @param context   The app context
     * @param activity  The activity for output
     * @param mService  The background service instance
     */
    ExportDatabaseToServerTask(Context context, Activity activity, MonitorService mService){
        m_context = context;
        m_service = mService;
        m_activity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(m_context, m_context.getString(R.string.export_progress_title), m_context.getString(R.string.export_server_start));
    }

    @Override
    protected ExportDatabaseStatus doInBackground(Void... params) {
        ServerCommunicationHandler sch = new ServerCommunicationHandler(m_context);

        publishProgress(ExportDatabaseStatus.CHECK_URL);

        if (!sch.areUrlsValid()){
            return ExportDatabaseStatus.CHECK_URL;
        }

        publishProgress(ExportDatabaseStatus.CHECK_NETWORK);
        if (!sch.isNetworkAvailable()){
            Log.d(TAG, "No network available");
            return ExportDatabaseStatus.CHECK_NETWORK;
        }

        publishProgress(ExportDatabaseStatus.REQUEST_TOKEN);
        String token = sch.requestToken(false);
        if (token == null){
            Log.d(TAG, "Cannot get token");
            return ExportDatabaseStatus.REQUEST_TOKEN;
        }
        Log.d(TAG, "TOKEN: " + token);

        publishProgress(ExportDatabaseStatus.REQUEST_RANGE);
        JSONObject job = sch.requestDataRange(token);
        if (job == null){
            // Mayne an issue with the token? request a new one and try again.
            token = sch.requestToken(true);
            if (token== null) {
                Log.d(TAG, "Cannot get new token");
                return ExportDatabaseStatus.REQUEST_RANGE;
            }
            job = sch.requestDataRange(token);
            if(job == null) {
                Log.d(TAG, "Error getting existing range");
                return ExportDatabaseStatus.REQUEST_RANGE;
            }
        }

        publishProgress(ExportDatabaseStatus.EXPORT_DB);
        JSONObject serverData = m_service.exportDataForServer(job);
        if (serverData == null){
            Log.d(TAG, "Error exporting data");
            return ExportDatabaseStatus.EXPORT_DB;
        }

        publishProgress(ExportDatabaseStatus.UPLOAD_DB);
        JSONObject uploadResult = sch.uploadData(serverData, token);
        if (uploadResult == null){
            Log.d(TAG, "Upload failed");
            return ExportDatabaseStatus.EXPORT_DB;
        }

        m_service.setServerUploadResult(uploadResult);

        publishProgress(ExportDatabaseStatus.DONE);

        return ExportDatabaseStatus.DONE;
    }

    protected void onProgressUpdate(ExportDatabaseStatus... values) {
        super.onProgressUpdate(values);
        switch(values[0]){
            case CHECK_URL:
                Log.d(TAG, "Checking URLs");
                progressDialog.setMessage(m_context.getString(R.string.export_server_check_url));
                break;
            case CHECK_NETWORK:
                Log.d(TAG, "Checking Network");
                progressDialog.setMessage(m_context.getString(R.string.export_server_check_network));
                break;
            case REQUEST_TOKEN:
                Log.d(TAG, "Requesting Token");
                progressDialog.setMessage(m_context.getString(R.string.export_server_request_token));
                break;
            case REQUEST_RANGE:
                Log.d(TAG, "Requesting existing datasets");
                progressDialog.setMessage(m_context.getString(R.string.export_server_check_existing_datasets));
                break;
            case EXPORT_DB:
                Log.d(TAG, "Exporting Data");
                progressDialog.setMessage(m_context.getString(R.string.export_server_export_data));
                break;
            case UPLOAD_DB:
                Log.d(TAG, "Uploading data");
                progressDialog.setMessage(m_context.getString(R.string.export_server_upload_data));
                break;
            case DONE:
                Log.d(TAG, "DONE");
                progressDialog.setMessage(m_context.getString(R.string.export_server_done));
                break;
            default:
                Log.d(TAG, "Unknown status code");
                progressDialog.setMessage(m_context.getString(R.string.export_server_unexpected_error));
        }
    }

    protected void onPostExecute(ExportDatabaseStatus status) {
        super.onPostExecute(status);
        progressDialog.dismiss();
        switch(status){
            case CHECK_URL:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_check_url_failed), m_context.getString(R.string.export_progress_title));
                break;
            case CHECK_NETWORK:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_check_network_failed), m_context.getString(R.string.export_progress_title));
                break;
            case REQUEST_TOKEN:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_request_token_failed), m_context.getString(R.string.export_progress_title));
                break;
            case REQUEST_RANGE:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_check_existing_datasets_failed), m_context.getString(R.string.export_progress_title));
                break;
            case EXPORT_DB:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_export_data_failed), m_context.getString(R.string.export_progress_title));
                break;
            case UPLOAD_DB:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_upload_data_failed), m_context.getString(R.string.export_progress_title));
                break;
            case DONE:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_done)+ "\n" + m_context.getString(R.string.export_server_changed_items) +  ": " + m_service.getLastServerUploadItems() + "\n\n" + m_context.getString(R.string.export_server_thanks), m_context.getString(R.string.export_progress_title));
                // Update Text view
                TextView lastUpload = (TextView) m_activity.findViewById(R.id.lastUpload);
                lastUpload.setText(m_context.getString(R.string.export_last_upload) +  ": " + m_service.getLastServerUploadTime());
                break;
            default:
                Helper.showUserMessage(m_context, m_context.getString(R.string.export_server_unexpected_error), m_context.getString(R.string.export_progress_title));
        }
    }
}