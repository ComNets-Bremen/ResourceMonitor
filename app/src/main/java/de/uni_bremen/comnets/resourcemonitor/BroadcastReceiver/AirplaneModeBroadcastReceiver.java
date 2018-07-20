package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;
import de.uni_bremen.comnets.resourcemonitor.MonitorService;

/**
 * BroadcastReceiver for the FlightMode
 */
public class AirplaneModeBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    public AirplaneModeBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db){
        super(monitorService, db);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        return intentFilter;
    }

    @Override
    public BroadcastReceiverDescriptor getReceiverDescription() {
        return new BroadcastReceiverDescriptor(
                "Airplane Mode",
                "This receiver records whether the airplane mode is switched on or off."
        );
    }

    @Override
    public void afterRegister(Context context) {
        super.afterRegister(context);

        ContentValues contentValues = new ContentValues();
        int airplaneMode = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, -1);
        contentValues.put(EnergyMonitorContract.AirplaneModeEntry.COLUMN_NAME_AIRPLANE_MODE, airplaneMode);

        storeValues(EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME, contentValues);
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
