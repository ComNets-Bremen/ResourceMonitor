package de.uni_bremen.comnets.resourcemonitor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
                implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    MonitorService mService;
    ProgressDialog progressDialog;
    boolean mBound = false;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Add the setting fragment to the main activity
        //getFragmentManager().beginTransaction().add(R.id.linearLayout, new SettingsFragment()).commit();

        // Generate a random UUID and store it to the preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("uuid", null) == null)
            preferences.edit().putString("uuid", UUID.randomUUID().toString()).apply();
        Log.d(TAG, "UUID: " + preferences.getString("uuid", "NONE"));

        // Ensure background service is running
        Intent serviceIntent = new Intent(this, MonitorService.class);
        startService(serviceIntent);

        Button exportButton = (Button) findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (mBound){
                    //exportDatabaseToMail();
                    exportDatabaseToServer();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.warn_connect_service), Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView uuidText = (TextView) findViewById(R.id.uuidText);
        uuidText.setText((uuidText.getText() + " " + preferences.getString("uuid", "None")));

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MonitorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);
        TextView tv = (TextView) findViewById(R.id.statusText);
        if (mBound) {
            tv.setText(mService.getDbStats());
            mService.updatedSetting("data_collection_enabled");
            // TODO: Update more settings?
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String text;
        switch (item.getItemId()) {
            case R.id.action_contact:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto",getString(R.string.mail_developer), null));
                String body = "\n\n" + "--" + "\n" +
                        getString(R.string.mail_developer_body) + "\n\n" +
                        "Version_Code: " + BuildConfig.VERSION_CODE + "\n" +
                        "Version_Name: " + BuildConfig.VERSION_NAME + "\n" +
                        "Language: " + Locale.getDefault().toString() + "\n" +
                        "Android: " + Build.VERSION.RELEASE;
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_developer_subject) + ": " + getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.dialog_mail_provider)));
                return true;
            case R.id.action_export_file:
                exportDatabaseToAnywhere();
                return true;
            case R.id.action_github:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_page)));
                startActivity(browserIntent);
                return true;
            case R.id.action_version:
                String s = getString(R.string.txt_version_name) + ": " + BuildConfig.VERSION_NAME + "\n" +
                        getString(R.string.txt_version_code) + ": " + BuildConfig.VERSION_CODE;
                showUserMessage(s, getString(R.string.txt_version_title));
                return true;
            case R.id.action_info:
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.infotext);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                showUserMessage(Html.fromHtml(text), getResources().getString(R.string.dialog_help_title));
                return true;

            case R.id.action_about_developer:
                try {
                    InputStream ioHelp = getResources().openRawResource(R.raw.about);
                    byte[] b = new byte[ioHelp.available()];
                    ioHelp.read(b);
                    text = new String(b);
                } catch (IOException e) {
                    text = getResources().getString(R.string.dialog_not_available);
                }

                showUserMessage(Html.fromHtml(text), getResources().getString(R.string.dialog_help_title));

                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorService.MonitorServiceBinder binder = (MonitorService.MonitorServiceBinder) service;
            mService = binder.getService();
            mBound = true;

            TextView tv = (TextView) findViewById(R.id.statusText);
            tv.setText(mService.getDbStats());
            mService.updatedSetting("data_collection_enabled");
            // TODO: Update more settings?
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mService.updatedSetting(key);
    }

    /**
     * Show a Message dialog to the user
     * @param s     The message as a String object
     * @param title The title of the message box
     */
    private void showUserMessage(String s, String title) {
        Spanned span = new SpannedString(s);
        showUserMessage(span, title);
    }

    /**
     * Show a Message dialog to the user
     * @param msg   The message as a Spanned object
     * @param title The title of the message box
     */
    private void showUserMessage(Spanned msg, String title) {
        AlertDialog dlgAlert = new AlertDialog.Builder(this)
                .setTitle(title)
                .setPositiveButton(getText(R.string.button_ok), null)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_info)
                .create();
        dlgAlert.show();
        TextView tv = (TextView) dlgAlert.findViewById(android.R.id.message);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setClickable(true);
    }

    /**
     * Export the database to anywhere using a thread to keep UI responding
     */
    private void exportDatabaseToAnywhere(){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        new exportDatabaseTask(intent, getResources().getText(R.string.dialog_export_provider).toString()).execute();
    }

    /**
     * Export the database via mail using a thread to keep UI responding
     */
    private void exportDatabaseToMail(){
        Intent emailTxIntent = new Intent(Intent.ACTION_SEND);
        emailTxIntent.setType("message/rfc822");
        emailTxIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.mail_destination)});
        emailTxIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.mail_subject));
        emailTxIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_message));

        new exportDatabaseTask(emailTxIntent, getResources().getText(R.string.dialog_mail_provider).toString()).execute();
    }

    private void exportDatabaseToServer(){
        new exportDatabaseToServerTask().execute();
    }

    /**
     * Class to export the database using an Android intent
     */
    private class exportDatabaseTask extends AsyncTask<Void, Void, Uri>{
        Intent m_intent;
        String m_dialogTitle;

        exportDatabaseTask(Intent intent, String dialogTitle){
            super();
            m_intent = intent;
            m_dialogTitle = dialogTitle;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, getString(R.string.export_progress_title),getString(R.string.export_progress_text), false);
        }

        @Override
        protected Uri doInBackground(Void... params) {
            File f = mService.exportData();
            return  FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileprovider", f);
        }

        @Override
        protected void onPostExecute(Uri uri) {
            super.onPostExecute(uri);
            progressDialog.dismiss();
            Log.d(TAG, "URI: " + uri);
            m_intent.putExtra(Intent.EXTRA_STREAM, uri);
            m_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(m_intent, m_dialogTitle));
        }
    }

    /**
     * Class to upload data to the server
     */
    private class exportDatabaseToServerTask extends AsyncTask<Void, Integer, Integer>{
        /* TODO
         * - separate module
         * - nicer status ids
         * - ...
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, getString(R.string.export_progress_title), getString(R.string.export_server_start));
        }

        @Override
        protected Integer doInBackground(Void... params) {
            ServerCommunicationHandler sch = new ServerCommunicationHandler(getApplicationContext());

            publishProgress(1);

            if (!sch.areUrlsValid()){
                return 1;
            }

            publishProgress(2);
            if (!sch.isNetworkAvailable()){
                Log.d(TAG, "No network available");
                return 2;
            }

            publishProgress(3);
            String token = sch.requestToken(false);
            if (token == null){
                Log.d(TAG, "Cannot get token");
                return 3;
            }
            Log.d(TAG, "TOKEN: " + token);

            publishProgress(4);
            JSONObject job = sch.requestDataRange(token);
            if (job == null){
                // Mayne an issue with the token? request a new one and try again.
                token = sch.requestToken(true);
                if (token== null) {
                    Log.d(TAG, "Cannot get new token");
                    return 3;
                }
                job = sch.requestDataRange(token);
                if(job == null) {
                    Log.d(TAG, "Error getting existing range");
                    return 4;
                }
            }

            publishProgress(5);
            JSONObject serverData = mService.exportDataForServer(job);
            if (serverData == null){
                Log.d(TAG, "Error exporting data");
                return 5;
            }

            publishProgress(6);
            JSONObject uploadResult = sch.uploadData(serverData, token);
            if (uploadResult == null){
                Log.d(TAG, "Upload failed");
                return 6;
            }

            publishProgress(7);

            return 0;
        }

        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch(values[0]){
                case 1:
                    Log.d(TAG, "Checking URLs");
                    progressDialog.setMessage(getString(R.string.export_server_check_url));
                    break;
                case 2:
                    Log.d(TAG, "Checking Network");
                    progressDialog.setMessage(getString(R.string.export_server_check_network));
                    break;
                case 3:
                    Log.d(TAG, "Requesting Token");
                    progressDialog.setMessage(getString(R.string.export_server_request_token));
                    break;
                case 4:
                    Log.d(TAG, "Requesting existing datasets");
                    progressDialog.setMessage(getString(R.string.export_server_check_existing_datasets));
                    break;
                case 5:
                    Log.d(TAG, "Exporting Data");
                    progressDialog.setMessage(getString(R.string.export_server_export_data));
                    break;
                case 6:
                    Log.d(TAG, "Uploading data");
                    progressDialog.setMessage(getString(R.string.export_server_upload_data));
                    break;
                case 7:
                    Log.d(TAG, "DONE");
                    progressDialog.setMessage(getString(R.string.export_server_done));
                    break;
                default:
                    Log.d(TAG, "Unknown status code");
                    progressDialog.setMessage(getString(R.string.export_server_unexpected_error));
            }
        }

        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);
            progressDialog.dismiss();
            switch(status){
                case 1:
                    showUserMessage(getString(R.string.export_server_check_url_failed), getString(R.string.export_progress_title));
                    break;
                case 2:
                    showUserMessage(getString(R.string.export_server_check_network_failed), getString(R.string.export_progress_title));
                    break;
                case 3:
                    showUserMessage(getString(R.string.export_server_request_token_failed), getString(R.string.export_progress_title));
                    break;
                case 4:
                    showUserMessage(getString(R.string.export_server_check_existing_datasets_failed), getString(R.string.export_progress_title));
                    break;
                case 5:
                    showUserMessage(getString(R.string.export_server_export_data_failed), getString(R.string.export_progress_title));
                    break;
                case 6:
                    showUserMessage(getString(R.string.export_server_upload_data_failed), getString(R.string.export_progress_title));
                    break;
                case 0:
                    showUserMessage(getString(R.string.export_server_done), getString(R.string.export_progress_title));
                    break;
                default:
                    showUserMessage(getString(R.string.export_server_unexpected_error), getString(R.string.export_progress_title));
            }
        }
    }
}

