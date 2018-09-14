package com.labs.dm.auto_tethering.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.ui.BluetoothLock;

import java.util.List;

import static com.labs.dm.auto_tethering.TetherIntents.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_DISCONNECTED;

/**
 * Created by Daniel Mroczka on 28-Mar-17.
 */

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothLock.fromTask) {
            BluetoothLock.fromTask = false;
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("bt.incoming.listen", false)) {
            BluetoothDevice bluetoothDevice;
            boolean isTethering = new ServiceHelper(context).isTetheringWiFi();
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    onActionConnected(context, intent, isTethering);
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    onActionDisconnected(context, intent, isTethering);
                    break;
            }
        }
    }

    private void onActionDisconnected(Context context, Intent intent, boolean isTethering) {
        BluetoothDevice bluetoothDevice;
        if (isTethering) {
            bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Intent btIntent = new Intent(BT_DISCONNECTED);
            btIntent.putExtra("name", bluetoothDevice.getName());
            Utils.broadcast(context, btIntent);
        }
        BluetoothLock.connectedFromReceiver = false;
    }

    private void onActionConnected(Context context, Intent intent, boolean isTethering) {
        BluetoothDevice bluetoothDevice;
        BluetoothLock.connectedFromReceiver = true;
        bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!isTethering) {
            List<BluetoothDevice> devicesToCheck = Utils.getBluetoothDevices(context, true);
            for (BluetoothDevice device : devicesToCheck) {
                if (device.getName() != null && device.getName().equals(bluetoothDevice.getName())) {
                    MyLog.i(TAG, "New connection to " + bluetoothDevice.getName());
                    Intent btIntent = new Intent(BT_CONNECTED);
                    btIntent.putExtra("name", bluetoothDevice.getName());
                    Utils.broadcast(context, btIntent);
                    break;
                }
            }
            if (devicesToCheck.isEmpty()) {
                Utils.showToast(context, "Bluetooth connection established but there are no preferred devices!");
            }
        }
    }
}
