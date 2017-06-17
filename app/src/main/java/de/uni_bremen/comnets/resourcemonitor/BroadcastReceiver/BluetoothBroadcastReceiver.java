package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;

public class BluetoothBroadcastReceiver extends ResourceBroadcastReceiver {

    public BluetoothBroadcastReceiver(SQLiteDatabase db){
        super(db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContentValues contentValues = new ContentValues();

        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            boolean hasBle = context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
            contentValues.put(EnergyMonitorContract.BluetoothStatusEntry.COLUMN_NAME_BLUETOOTH_STATUS, state);
            contentValues.put(EnergyMonitorContract.BluetoothStatusEntry.COLUMN_NAME_BLE_AVAILABLE, hasBle);
        }

        storeValues(EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME, contentValues);
    }

    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }
}
