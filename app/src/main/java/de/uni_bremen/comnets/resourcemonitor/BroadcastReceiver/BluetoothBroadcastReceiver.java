package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;

/**
 * BroadcastReceiver for Bluetooth events
 */
public class BluetoothBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    public BluetoothBroadcastReceiver(SQLiteDatabase db){
        super(db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContentValues contentValues = new ContentValues();

        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            contentValues.put(EnergyMonitorContract.BluetoothStatusEntry.COLUMN_NAME_BLUETOOTH_STATUS, state);
        }

        storeValues(EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME, contentValues);
    }

    @Override
    public void afterRegister(Context context) {
        super.afterRegister(context);

        ContentValues contentValues = new ContentValues();

        int state = BluetoothAdapter.getDefaultAdapter().getState();
        contentValues.put(EnergyMonitorContract.BluetoothStatusEntry.COLUMN_NAME_BLUETOOTH_STATUS, state);
        storeValues(EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME, contentValues);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }
}
