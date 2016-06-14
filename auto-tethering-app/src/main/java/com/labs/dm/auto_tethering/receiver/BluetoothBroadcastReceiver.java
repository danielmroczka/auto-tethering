package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static com.labs.dm.auto_tethering.TetherInvent.BT_FOUND_END;
import static com.labs.dm.auto_tethering.TetherInvent.BT_FOUND_NEW;
import static com.labs.dm.auto_tethering.TetherInvent.BT_FOUND_START;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "BT Broadcast Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, intent.getAction());
        Intent btIntent = null;

        switch (intent.getAction()) {
            case ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                if (device != null) {
                    Log.i(TAG, "Found devices: " + device.getName());
                    btIntent = new Intent(BT_FOUND_NEW);
                    btIntent.putExtra("device", device.getName());
                }
                break;
            case ACTION_DISCOVERY_STARTED:
                btIntent = new Intent(BT_FOUND_START);
                break;
            case ACTION_DISCOVERY_FINISHED:
                btIntent = new Intent(BT_FOUND_END);
                break;
        }

        if (btIntent != null) {
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
