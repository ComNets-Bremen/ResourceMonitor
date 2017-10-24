package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class to handle the data upload to the server
 */

public class ServerCommunicationHandler {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int TIMEOUT = 30000;

    private Context context;

    private URL uploadUrl = null;
    private URL tokenUrl = null;
    private URL rangeUrl = null;

    private String uuid;

    ServerCommunicationHandler(Context context){
        this.context = context;

        try {
            uploadUrl = new URL(context.getString(R.string.url_export_server_upload));
            tokenUrl = new URL(context.getString(R.string.url_export_server_token));
            rangeUrl = new URL(context.getString(R.string.url_export_server_range));
        } catch (MalformedURLException e) {
            uploadUrl = tokenUrl = rangeUrl = null;
        }

        uuid = PreferenceManager.getDefaultSharedPreferences(context).getString("uuid", null);
    }

    /**
     * Check if the configured URLs are valid
     *
     * @return true if valid, false otherwise
     */
    public Boolean areUrlsValid(){
        return !(uploadUrl == null || tokenUrl == null || rangeUrl == null);
    }

    /**
     * Check if a network connection is available
     *
     * @return true if we have a network connection
     */
    public Boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Request a token
     *
     * @param forceRefresh force to get a token from the server. Otherwise use the locally stored one
     * @return  The token or null if an error occurred
     */
    public String requestToken(Boolean forceRefresh){
        String token = PreferenceManager.getDefaultSharedPreferences(context).getString("token", null);

        if (token == null || forceRefresh){
            HttpURLConnection connection = null;
            try{
                String url = tokenUrl.toString();
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length()-1);
                }
                if (!url.endsWith("?")) {
                    url += "?";
                }
                url += "id=" + uuid;

                URL requestUrl = new URL(url);
                Log.d(TAG, "Token URL: " + url);

                connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setConnectTimeout(TIMEOUT);
                InputStream is = new BufferedInputStream(connection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null){
                    sb.append(inputStr);
                }

                JSONObject job = new JSONObject(sb.toString());
                if (job.has("TOKEN")){
                    Log.d(TAG, "Got new token");
                    token = job.getString("TOKEN");
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("token", token).apply();
                } else {
                    // Invalid response
                    return null;
                }

                connection.disconnect();
            } catch (Exception e) {
                // HTTP Error
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        return token;
    }

    /**
     * Request the range of data expected by the server
     *
     * @param token a valid token
     * @return a json object with the requested data
     */
    public JSONObject requestDataRange(String token){
        Log.d(TAG, "Range URL: " + rangeUrl);
        HttpURLConnection connection = null;
        JSONObject receivedData = null;

        try {
            connection = (HttpURLConnection) rangeUrl.openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-CLIENTID", uuid);

            String hmac = getBase64Hmac(token, token);
            if (hmac == null){
                connection.setRequestProperty("X-TOKEN", token);
            } else {
                connection.setRequestProperty("X-SECRET", hmac);
            }

            Log.d(TAG, "SECRET: " + hmac);

            InputStream is = new BufferedInputStream(connection.getInputStream());
            BufferedReader bsr = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String inputString;
            while ((inputString = bsr.readLine()) != null){
                sb.append(inputString);
            }
            Log.d(TAG, "DATA: " + sb.toString());
            receivedData = new JSONObject(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            receivedData = null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return receivedData;
    }

    /**
     * Upload the data to the server
     *
     * @param serverData    The data to upload
     * @param token         a valid token
     * @return              The number of uploaded items
     */
    public JSONObject uploadData(JSONObject serverData, String token) {
        HttpURLConnection connection = null;
        JSONObject receivedData = null;

        try {
            connection = (HttpURLConnection) uploadUrl.openConnection();
            connection.setConnectTimeout(TIMEOUT);

            connection.setRequestProperty("X-CLIENTID", uuid);
            connection.setRequestProperty("Content-type", "application/json");

            connection.setDoOutput(true);

            String postData = serverData.toString();

            String hmac = getBase64Hmac(token, postData);
            if (hmac == null){
                connection.setRequestProperty("X-TOKEN", token);
            } else {
                connection.setRequestProperty("X-SECRET", hmac);
            }
            connection.setFixedLengthStreamingMode(postData.length());

            OutputStream os = new BufferedOutputStream(connection.getOutputStream());
            PrintStream ps = new PrintStream(os);
            ps.print(postData);
            ps.flush();

            InputStream is = new BufferedInputStream(connection.getInputStream());
            BufferedReader bsr = new BufferedReader(new InputStreamReader(is));

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = bsr.readLine()) != null) {
                sb.append(line);
            }

            receivedData = new JSONObject(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return  receivedData;
    }


    /**
     * Create a Base64 encoded HMAC
     * @param token The token
     * @param data  The data
     * @return      The base64 encoded resulting hmac
     */
    private static String getBase64Hmac(String token, String data){

        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec sk = new SecretKeySpec(token.getBytes(),mac.getAlgorithm());
            mac.init(sk);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }

        byte result[] = mac.doFinal(data.getBytes());

        return Base64.encodeToString(result, Base64.URL_SAFE);
    }
}
