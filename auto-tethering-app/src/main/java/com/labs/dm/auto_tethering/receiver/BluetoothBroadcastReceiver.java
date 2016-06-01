package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Toast.makeText(context, "Bluetooth found " + device.getName(), Toast.LENGTH_LONG).show();
            Log.i("BT Broadcast Receiver", device.getName());

            Intent btIntent = new Intent("bt.ready");
            btIntent.putExtra("device", device.getName());
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

        if (ACTION_DISCOVERY_STARTED.equals(action)) {
            Log.i("BT Broadcast Receiver", "Discovery Started");
            Intent btIntent = new Intent("bt.ready");
            btIntent.putExtra("clear", true);
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

        if (ACTION_DISCOVERY_FINISHED.equals(action)) {
            Log.i("BT Broadcast Receiver", "Discovery Finished");
        }
    }
}
