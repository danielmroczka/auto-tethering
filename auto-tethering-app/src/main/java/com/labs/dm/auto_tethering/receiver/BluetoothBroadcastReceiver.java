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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("bt.incoming.listen", false)) {
            BluetoothDevice bluetoothDevice;
            boolean isTethering = new ServiceHelper(context).isTetheringWiFi();
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
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
                    }
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    if (isTethering) {
                        bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Intent btIntent = new Intent(BT_DISCONNECTED);
                        btIntent.putExtra("name", bluetoothDevice.getName());
                        Utils.broadcast(context, btIntent);
                    }
                    break;
            }
        }
    }
}
