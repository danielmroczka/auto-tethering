package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static com.labs.dm.auto_tethering.TetherInvent.*;

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
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                btIntent = new Intent(BT_CONNECTED);
                BluetoothDevice dv = intent.getParcelableExtra(EXTRA_DEVICE);
                btIntent.putExtra("name", dv.getName());
                Log.i("BT", "connected to " + dv.getName());
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                btIntent = new Intent(BT_DISCONNECTED);
                BluetoothDevice dv1 = intent.getParcelableExtra(EXTRA_DEVICE);
                btIntent.putExtra("name", dv1.getName());
                Log.i("BT", "disconnect from " + dv1.getName());
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                btIntent = new Intent(BT_DISCONNECTED);
                BluetoothDevice dv2 = intent.getParcelableExtra(EXTRA_DEVICE);
                btIntent.putExtra("name", dv2.getName());
                Log.i("BT", "disconnect request from " + dv2.getName());
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
