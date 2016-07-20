package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Created by Daniel Mroczka
 */
public class ChargeBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "USB Broadcast Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        Intent usbIntent = null;
        Log.i(TAG, intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_POWER_CONNECTED:
                usbIntent = new Intent(TetherIntents.USB_ON);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("usb.internet.start.service", false) && !helper.isServiceRunning(TetheringService.class)) {
                    Intent serviceIntent = new Intent(context, TetheringService.class);
                    serviceIntent.putExtra("usb.on", true);
                    context.startService(serviceIntent);
                }
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                if (helper.isTetheringWiFi()) {
                    usbIntent = new Intent(TetherIntents.USB_OFF);
                }
                break;
        }

        Utils.broadcast(context, usbIntent);
    }
}
