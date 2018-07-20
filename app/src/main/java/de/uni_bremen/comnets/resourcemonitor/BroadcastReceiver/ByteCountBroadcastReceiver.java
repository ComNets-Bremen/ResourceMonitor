package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;
import de.uni_bremen.comnets.resourcemonitor.MonitorService;

/**
 * BroadcastReceiver for the number of transmitted bytes.
 * Triggered if WiFi / cellular interface changes the state
 */
public class ByteCountBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    public ByteCountBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db){
        super(monitorService, db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EnergyMonitorContract.TrafficStatsEntry.COLUMN_NAME_MOBILE_RX, TrafficStats.getMobileRxBytes());
        contentValues.put(EnergyMonitorContract.TrafficStatsEntry.COLUMN_NAME_MOBILE_TX, TrafficStats.getMobileTxBytes());
        contentValues.put(EnergyMonitorContract.TrafficStatsEntry.COLUMN_NAME_TOTAL_RX, TrafficStats.getTotalRxBytes());
        contentValues.put(EnergyMonitorContract.TrafficStatsEntry.COLUMN_NAME_TOTAL_TX, TrafficStats.getTotalTxBytes());

        storeValues(EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME, contentValues);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        return intentFilter;
    }

    @Override
    public BroadcastReceiverDescriptor getReceiverDescription() {
        return new BroadcastReceiverDescriptor(
                "Traffic Receiver",
                "This receiver collects data traffic related values. It stores the number of bytes sent and received in total and via the mobile connection."
        );
    }

}
