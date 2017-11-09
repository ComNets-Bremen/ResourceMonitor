package de.uni_bremen.comnets.resourcemonitor;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Class to upload data to the server
 */
class ExportDatabaseToServerTask extends AsyncTask<Void, Integer, Integer> {

    private static final String TAG = ExportDatabaseToServerTask.class.getSimpleName();
    private ProgressDialog progressDialog = null;
    private WeakReference<Context> m_context = null;
    private WeakReference<MonitorService> m_service = null;
    private Boolean m_background;

    /* TODO
     * - nicer status ids
     * - ...
     */

    /**
     *
     * Default constructor, possibility to run as background service
     *
     * @param context   The app context
     * @param mService  The background service instance
     * @param doInBackground   Run the task in the background
     */
    public ExportDatabaseToServerTask(Context context, MonitorService mService, Boolean doInBackground){
        m_context = new WeakReference<Context>(context);
        m_service = new WeakReference<MonitorService>(mService);
        m_background = doInBackground;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!m_background && m_context != null) {
            progressDialog = ProgressDialog.show(m_context.get(), m_context.get().getString(R.string.export_progress_title), m_context.get().getString(R.string.export_server_start));
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        ServerCommunicationHandler sch = new ServerCommunicationHandler(m_context.get());

        publishProgress(ServerCommunicationHandler.CHECK_URL);

        if (!sch.areUrlsValid()){
            return ServerCommunicationHandler.CHECK_URL;
        }

        publishProgress(ServerCommunicationHandler.CHECK_NETWORK);
        if (!sch.isNetworkAvailable()){
            Log.d(TAG, "No network available");
            return ServerCommunicationHandler.CHECK_NETWORK;
        }

        publishProgress(ServerCommunicationHandler.REQUEST_TOKEN);
        String token = sch.requestToken(false);
        if (token == null){
            Log.d(TAG, "Cannot get token");
            return ServerCommunicationHandler.REQUEST_TOKEN;
        }
        Log.d(TAG, "TOKEN: " + token);

        publishProgress(ServerCommunicationHandler.REQUEST_RANGE);
        JSONObject job = sch.requestDataRange(token);
        if (job == null){
            // Mayne an issue with the token? request a new one and try again.
            token = sch.requestToken(true);
            if (token== null) {
                Log.d(TAG, "Cannot get new token");
                return ServerCommunicationHandler.REQUEST_RANGE;
            }
            job = sch.requestDataRange(token);
            if(job == null) {
                Log.d(TAG, "Error getting existing range");
                return ServerCommunicationHandler.REQUEST_RANGE;
            }
        }

        publishProgress(ServerCommunicationHandler.EXPORT_DB);
        JSONObject serverData = null;
        if (m_service != null) {
            serverData = m_service.get().exportDataForServer(job);
        }
        if (serverData == null){
            Log.d(TAG, "Error exporting data");
            return ServerCommunicationHandler.EXPORT_DB;
        }

        publishProgress(ServerCommunicationHandler.UPLOAD_DB);
        JSONObject uploadResult = sch.uploadData(serverData, token);
        if (uploadResult == null){
            Log.d(TAG, "Upload failed");
            return ServerCommunicationHandler.EXPORT_DB;
        }

        if (m_service != null) {
            m_service.get().setServerUploadResult(uploadResult);
        }

        publishProgress(ServerCommunicationHandler.DONE);

        return ServerCommunicationHandler.DONE;
    }

    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (!m_background && m_context != null) {
            Context c = m_context.get();
            switch (values[0]) {
                case ServerCommunicationHandler.CHECK_URL:
                    Log.d(TAG, "Checking URLs");
                    progressDialog.setMessage(c.getString(R.string.export_server_check_url));
                    break;
                case ServerCommunicationHandler.CHECK_NETWORK:
                    Log.d(TAG, "Checking Network");
                    progressDialog.setMessage(c.getString(R.string.export_server_check_network));
                    break;
                case ServerCommunicationHandler.REQUEST_TOKEN:
                    Log.d(TAG, "Requesting Token");
                    progressDialog.setMessage(c.getString(R.string.export_server_request_token));
                    break;
                case ServerCommunicationHandler.REQUEST_RANGE:
                    Log.d(TAG, "Requesting existing datasets");
                    progressDialog.setMessage(c.getString(R.string.export_server_check_existing_datasets));
                    break;
                case ServerCommunicationHandler.EXPORT_DB:
                    Log.d(TAG, "Exporting Data");
                    progressDialog.setMessage(c.getString(R.string.export_server_export_data));
                    break;
                case ServerCommunicationHandler.UPLOAD_DB:
                    Log.d(TAG, "Uploading data");
                    progressDialog.setMessage(c.getString(R.string.export_server_upload_data));
                    break;
                case ServerCommunicationHandler.DONE:
                    Log.d(TAG, "DONE");
                    progressDialog.setMessage(c.getString(R.string.export_server_done));
                    break;
                default:
                    Log.d(TAG, "Unknown status code");
                    progressDialog.setMessage(c.getString(R.string.export_server_unexpected_error));
            }
        }
    }

    protected void onPostExecute(Integer status) {
        super.onPostExecute(status);
        if (progressDialog != null) {
            progressDialog.dismiss();
            Log.d(TAG, "DISMISS");
        }

        Context c = null;
        if (m_context != null){
            c = m_context.get();
        }

        switch(status){
            case ServerCommunicationHandler.CHECK_URL:
                if (!m_background && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_check_url_failed), c.getString(R.string.export_progress_title));
                }
                break;
            case ServerCommunicationHandler.CHECK_NETWORK:
                if (!m_background && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_check_network_failed), c.getString(R.string.export_progress_title));
                }
                break;
            case ServerCommunicationHandler.REQUEST_TOKEN:
                if (!m_background && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_request_token_failed), c.getString(R.string.export_progress_title));
                }
                break;
            case ServerCommunicationHandler.REQUEST_RANGE:
                if (!m_background && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_check_existing_datasets_failed), c.getString(R.string.export_progress_title));
                }
                break;
            case ServerCommunicationHandler.EXPORT_DB:
                if (!m_background && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_export_data_failed), c.getString(R.string.export_progress_title));
                }
                break;
            case ServerCommunicationHandler.UPLOAD_DB:
                if (!m_background && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_upload_data_failed), c.getString(R.string.export_progress_title));
                }
                break;
            case ServerCommunicationHandler.DONE:
                if (!m_background && m_service != null && c != null) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_done) + "\n" + c.getString(R.string.export_server_changed_items) + ": " + m_service.get().getLastServerUploadItems() + "\n\n" + c.getString(R.string.export_server_thanks), c.getString(R.string.export_progress_title));
                }
                if (m_service != null) {
                    m_service.get().updateNotification();
                }
                break;
            default:
                if (!m_background) {
                    Helper.showUserMessage(c, c.getString(R.string.export_server_unexpected_error), c.getString(R.string.export_progress_title));
                }
        }

        if (m_service != null){
            // Store last status
            m_service.get().setLastServerUploadStatuscode(status);
        }

        // Ensure everything is tidied up after usage
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

        if (m_service != null){
            m_service.clear();
            m_service = null;
        }
    }
}