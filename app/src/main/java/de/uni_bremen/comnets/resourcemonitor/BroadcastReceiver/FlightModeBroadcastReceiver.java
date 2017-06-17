package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;

public class FlightModeBroadcastReceiver extends ResourceBroadcastReceiver {

    public FlightModeBroadcastReceiver(SQLiteDatabase db){
        super(db);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContentValues contentValues = new ContentValues();
        if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)){
            boolean inAirplaneMode = intent.getBooleanExtra("state", false);
            contentValues.put(EnergyMonitorContract.AirplaneModeEntry.COLUMN_NAME_AIRPLANE_MODE, inAirplaneMode);
        }

        storeValues(EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME, contentValues);
    }
}
