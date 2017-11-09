package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

import de.uni_bremen.comnets.resourcemonitor.MonitorService;

import static de.uni_bremen.comnets.resourcemonitor.Helper.isPowerSaving;

/**
 * Abstract class for BroadcastReceivers on changed devices parameters. Offers
 *
 * - Access to database
 * - do not store duplicate values
 * - Register / unregister handler
 * - Generic TAG for logging
 */

public abstract class AbstractResourceBroadcastReceiver extends BroadcastReceiver {

    public String TAG;
    private AbstractResourceBroadcastReceiver mReceiver = null;
    private ContentValues lastContentValues;
    private MonitorService mService = null;

    private SQLiteDatabase writableDb;

    /**
     * Constructor, db needed
     * @param db A writable database
     */
    public AbstractResourceBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db){
        super();
        lastContentValues = new ContentValues();
        writableDb = db;
        mService = monitorService;
        TAG = this.getClass().getSimpleName();
    }

    /**
     * Children have to implement this function to define the intentFilter required by them
     *
     * @param intentFilter The intentFilter which has to be adapted and returned afterwards
     * @return the returned intentFilter
     */
    public abstract IntentFilter getIntentFilter(IntentFilter intentFilter);


    /**
     * Store values into db
     * @param tableName The table where to store the values
     * @param contentValues The values to be stored in the db
     * @return The line where the values where stored in. -1 if storing was not possible / required
     */
    public long storeValues(String tableName, ContentValues contentValues){
        long row = -1;

        /*
        if (isPowerSaving(mService) && ! mService.isNotificationEnabled()){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mService);
            preferences.edit().putBoolean("show_notification_bar", true).apply();
            mService.updatedSetting("show_notification_bar");
        }
*/
        // Do not store duplicated values
        if (!lastContentValues.equals(contentValues)){
            row = writableDb.insert(tableName, null, contentValues);
            Log.d(TAG, "INSERT row " + row + ": " + contentValues);
            lastContentValues = contentValues;
            mService.setDatasetStored();
        }

        return row;
    }

    /**
     * Called after the receiver was successfully registered.
     * Has to be overwritten by the implementing class
     */
    public void afterRegister(Context context){}

    /**
     * Register this receiver to the given context
     * @param context
     */
    public void register(Context context){
        Log.d(TAG, "register");
        if (mReceiver == null){
            //mReceiver = new WiFiBroadcastReceiver(writableDb);

            try {
                mReceiver = this.getClass().getConstructor(MonitorService.class, SQLiteDatabase.class).newInstance(mService, writableDb);
            } catch (Exception e) {
                e.printStackTrace();
            }

            IntentFilter intentFilter = new IntentFilter();
            context.registerReceiver(mReceiver, getIntentFilter(intentFilter));
            afterRegister(context);
        }
    }

    /**
     * Unregister this receiver from the given context
     * @param context
     */
    public void unregister(Context context){
        Log.d(TAG, "unregister");
        if (mReceiver != null){
            context.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    /**
     * Get the instance of the MonitorService
     *
     * @return  The MonitorService instance
     */
    protected MonitorService getMonitorServiceInstance(){
        return mService;
    }

    /**
     * Get the writable database instance
     *
     * @return The database instance
     */
    protected SQLiteDatabase getWritableDbInstance(){
        return writableDb;
    }
}
