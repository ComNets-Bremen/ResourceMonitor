package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;
import de.uni_bremen.comnets.resourcemonitor.MonitorService;

/**
 * BroadcastReceiver for the WiFi network status
 */
public class WiFiBroadcastReceiver extends AbstractResourceBroadcastReceiver {

    public WiFiBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db) {
        super(monitorService, db);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();

            ContentValues contentValues = new ContentValues();
            contentValues.put(
                    EnergyMonitorContract.WiFiStatusEntry.COLUMN_NAME_WIFI_STATUS,
                    DetailedStateToNum(detailedState)
            );
            storeValues(EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME, contentValues);
        }
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter){
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return intentFilter;
    }

    @Override
    public BroadcastReceiverDescriptor getReceiverDescription() {
        return new BroadcastReceiverDescriptor(
                "WiFi Status",
                "This receiver collects data regaring the current WiFi status. No SSIDs or MAC addresses are stored. The possible values are: AUTHENTICATING, BLOCKED, CAPTIVE_PORTAL_CHECK, CONNECTED, CONNECTING, DISCONNECTED, FAILED, IDLE, OBTAINING_IPADDR, SCANNING, VERIFYING_POOR_LINK");
    }


    /**
     * Convert the enum values to integer values to reduce the database overhead
     *
     * @param state the DetailedState
     * @return integer representing the state
     */
    public static int DetailedStateToNum(NetworkInfo.DetailedState state){
        switch (state){
            case AUTHENTICATING:
                return 1;
            case BLOCKED:
                return 2;
            case CAPTIVE_PORTAL_CHECK:
                return 3;
            case CONNECTED:
                return 4;
            case CONNECTING:
                return 5;
            case DISCONNECTED:
                return 6;
            case FAILED:
                return 7;
            case IDLE:
                return 8;
            case OBTAINING_IPADDR:
                return 9;
            case SCANNING:
                return 10;
            case SUSPENDED:
                return 11;
            case VERIFYING_POOR_LINK:
                return 12;
            default:
                return -1;

        }
    }
}
