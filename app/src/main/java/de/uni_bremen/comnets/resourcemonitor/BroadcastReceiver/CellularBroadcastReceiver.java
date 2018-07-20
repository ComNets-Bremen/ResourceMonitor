package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import de.uni_bremen.comnets.resourcemonitor.EnergyMonitorContract;
import de.uni_bremen.comnets.resourcemonitor.MonitorService;


/**
 * BroadcastReceiver for cellular events
 */
public class CellularBroadcastReceiver extends AbstractResourceBroadcastReceiver {
    public CellularBroadcastReceiver(MonitorService monitorService, SQLiteDatabase db){
        super(monitorService, db);
    }

    @Override
    public IntentFilter getIntentFilter(IntentFilter intentFilter) {
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return intentFilter;
    }

    @Override
    public BroadcastReceiverDescriptor getReceiverDescription() {
        return new BroadcastReceiverDescriptor(
                "Cellular Interface Status",
                "This receiver collects data regarding the mobile connection. It stores the status (i.e. CONNECTED, CONNECTING, DISCONNECTED, DISCONNECTING, SUSPENDED) and the name of the type of the connection (i.e. LTE; GSM, 3G etc.)."
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        ContentValues contentValues = new ContentValues();
        NetworkInfo networkInfo = null;

        // Access to the network info has changed in LOLLIPOP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Network[] networks = conn.getAllNetworks();
            for (Network n: networks){
                networkInfo = conn.getNetworkInfo(n);
                if (!(networkInfo == null)) {
                    // We are only interested in the mobile network
                    if (networkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
                        networkInfo = null;
                    }
                }
            }

        } else {
            networkInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        }

        String connType;
        int connState;

        if (networkInfo == null){
            // No cellular connectivity
            connType = "NONE";
            connState = -1;
        } else {
            connType = networkInfo.getSubtypeName();
            connState = StateToNum(networkInfo.getState());
        }

        contentValues.put(EnergyMonitorContract.CellularStatusEntry.COLUMN_NAME_TYPE, connType);
        contentValues.put(EnergyMonitorContract.CellularStatusEntry.COLUMN_NAME_STATE, connState);

        storeValues(EnergyMonitorContract.CellularStatusEntry.TABLE_NAME, contentValues);
    }

    /**
     * Convert the enum values to integer values to reduce the database overhead
     *
     * @param state the DetailedState
     * @return integer representing the state
     */
    public static int StateToNum(NetworkInfo.State  state){
        switch (state){
            case CONNECTED:
                return 4;
            case CONNECTING:
                return 5;
            case DISCONNECTED:
                return 6;
            case DISCONNECTING:
                return 7;
            case SUSPENDED:
                return 11;
            default:
                return -1;

        }
    }
}
