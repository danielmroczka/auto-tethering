package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static com.labs.dm.auto_tethering.TetherInvent.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherInvent.BT_DISCONNECTED;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "BT Broadcast Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, intent.getAction());
        Intent btIntent = null;

        switch (intent.getAction()) {
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
