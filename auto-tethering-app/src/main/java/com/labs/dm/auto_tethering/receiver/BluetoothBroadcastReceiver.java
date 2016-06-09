package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BT Broadcast Receiver", intent.getAction());
        String action = intent.getAction();
        Intent btIntent = null;
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                //Toast.makeText(context, "Bluetooth found " + device.getName(), Toast.LENGTH_LONG).show();
                Log.i("BT BroadcastReceiver", "Found devices: " + device.getName());
                btIntent = new Intent("bt.found.new");
                btIntent.putExtra("device", device.getName());
            }
        }

        if (ACTION_DISCOVERY_STARTED.equals(action)) {
            btIntent = new Intent("bt.found.start");
        }

        if (ACTION_DISCOVERY_FINISHED.equals(action)) {
            btIntent = new Intent("bt.found.end");
        }

        if (btIntent != null) {
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }
}
