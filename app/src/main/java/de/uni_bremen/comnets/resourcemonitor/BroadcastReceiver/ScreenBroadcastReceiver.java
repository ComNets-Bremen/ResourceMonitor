package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;

/**
 * BroadcastReceiver for the screen status
 */
public class ScreenBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    public ScreenBroadcastReceiver(SQLiteDatabase db){
        super(db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean screenOn;

        if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
            screenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
            screenOn = true;
        } else {
            return;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(EnergyMonitorContract.ScreenStatusEntry.COLUMN_NAME_SCREEN_STATUS, screenOn);

        storeValues(EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME, contentValues);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);

        return intentFilter;
    }
}
