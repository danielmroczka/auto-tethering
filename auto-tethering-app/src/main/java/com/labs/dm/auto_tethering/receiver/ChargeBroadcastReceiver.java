package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.labs.dm.auto_tethering.service.ServiceHelper;

public class ChargeBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        ServiceHelper helper = new ServiceHelper(context);
        Intent usbIntent = null;
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            usbIntent = new Intent("usb.on");
            Log.i("usb", "onConnect");
            Toast.makeText(context, "USB on", Toast.LENGTH_LONG).show();
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED) && helper.isSharingWiFi()) {
            usbIntent = new Intent("usb.off");
            Log.i("usb", "onDisconnect");
            Toast.makeText(context, "USB off", Toast.LENGTH_LONG).show();
        }

        if (usbIntent != null) {
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, usbIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

    }
}
