package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.USB_OFF;
import static com.labs.dm.auto_tethering.TetherIntents.USB_ON;

/**
 * Created by Daniel Mroczka
 */
public class ChargeBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "USB Broadcast Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        Intent usbIntent = null;
        MyLog.i(TAG, intent.getAction());

        switch (intent.getAction()) {
            case ACTION_POWER_CONNECTED:
                usbIntent = new Intent(USB_ON);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("usb.internet.start.service", false) && !helper.isServiceRunning(TetheringService.class)) {
                    Intent serviceIntent = new Intent(context, TetheringService.class);
                    serviceIntent.putExtra("usb.on", true);
                    context.startService(serviceIntent);
                }
                break;
            case ACTION_POWER_DISCONNECTED:
                if (helper.isTetheringWiFi()) {
                    usbIntent = new Intent(USB_OFF);
                }
                break;
        }

        if (usbIntent != null) {
            Utils.broadcast(context, usbIntent);
        }
    }
}
