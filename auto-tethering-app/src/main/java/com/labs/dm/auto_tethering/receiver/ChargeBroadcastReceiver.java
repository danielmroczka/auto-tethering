package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.labs.dm.auto_tethering.TetherInvent;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

public class ChargeBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "USB Broadcast Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        Intent usbIntent = null;
        Log.i(TAG, intent.getAction());
        switch (intent.getAction()) {
            case Intent.ACTION_POWER_CONNECTED: {
                usbIntent = new Intent(TetherInvent.USB_ON);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("usb.internet.start.service", false) && !helper.isServiceRunning(TetheringService.class)) {
                    Intent serviceIntent = new Intent(context, TetheringService.class);
                    serviceIntent.putExtra("usb.on", true);
                    context.startService(serviceIntent);
                }

            }
            case Intent.ACTION_POWER_DISCONNECTED: {
                if (helper.isSharingWiFi()) {
                    usbIntent = new Intent(TetherInvent.USB_OFF);
                }
            }
        }

        if (usbIntent != null) {
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, usbIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }
}
