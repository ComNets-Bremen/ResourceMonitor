package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;

/**
 * BroadcastReceiver for the Energy sources used by this device
 */
public class PowerBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    public PowerBroadcastReceiver(SQLiteDatabase db){
        super(db);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
        boolean wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = level / (float) scale;

        ContentValues contentValues = new ContentValues();
        contentValues.put(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_CHG_AC, acCharge);
        contentValues.put(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_CHG_USB, usbCharge);
        contentValues.put(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_CHG_WIRELESS, wirelessCharge);
        contentValues.put(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_PERCENTAGE, batteryPercentage);
        contentValues.put(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_IS_CHARGING, isCharging);

        storeValues(EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME, contentValues);
    }
}
