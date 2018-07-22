package de.uni_bremen.comnets.resourcemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver to start the app on bootup
 */

public class MonitorServiceReceiver extends BroadcastReceiver {
    public static final String TAG = MonitorServiceReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "Received intent: " +intent.getAction());
        // TODO Check action before doing something

        Intent serviceIntent = new Intent(context, MonitorService.class);
        context.startService(serviceIntent);
    }
}
