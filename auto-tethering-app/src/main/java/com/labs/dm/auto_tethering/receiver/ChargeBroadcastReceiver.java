package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import com.labs.dm.auto_tethering.R;

public class ChargeBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            Intent onIntent = new Intent("usb.on");
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
            Log.i("usb", "onConnect");
            Toast.makeText(context, "USB on", Toast.LENGTH_LONG).show();
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            Intent onIntent = new Intent("usb.off");
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
            Log.i("usb", "onDisconnect");
            Toast.makeText(context, "USB off", Toast.LENGTH_LONG).show();
        }
    }
}
