package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;

/**
 * Created by jd on 17.06.17.
 */

public abstract class ResourceBroadcastReceiver extends BroadcastReceiver {

    private ResourceBroadcastReceiver mReceiver = null;

    private ContentValues lastContentValues;

    public String TAG;

    SQLiteDatabase writableDb;

    public ResourceBroadcastReceiver(SQLiteDatabase db){
        super();
        lastContentValues = new ContentValues();
        writableDb = db;
        TAG = this.getClass().getSimpleName();
    }

    public abstract IntentFilter getIntentFilter(IntentFilter intentFilter);

    public long storeValues(String tableName, ContentValues contentValues){
        long row = -1;

        // Do not store duplicated values
        if (!lastContentValues.equals(contentValues)){
            row = writableDb.insert(tableName, null, contentValues);
            Log.d(TAG, "INSERT row " + row + ": " + contentValues);
            lastContentValues = contentValues;
        }

        return row;
    }

    public void register(Context context){
        Log.d(TAG, "register");
        if (mReceiver == null){
            //mReceiver = new WiFiBroadcastReceiver(writableDb);

            try {
                mReceiver = this.getClass().getConstructor(SQLiteDatabase.class).newInstance(writableDb);
            } catch (Exception e) {
                e.printStackTrace();
            }

            IntentFilter intentFilter = new IntentFilter();
            context.registerReceiver(mReceiver, getIntentFilter(intentFilter));
        }
    }

    public void unregister(Context context){
        Log.d(TAG, "unregister");
        if (mReceiver != null){
            context.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }
}
