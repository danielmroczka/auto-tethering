package com.labs.dm.auto_tethering.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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
            int state;
            BluetoothDevice bluetoothDevice;

            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    //case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        Toast.makeText(context, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                        Log.d("BroadcastActions", "Bluetooth is off");
                    } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        Toast.makeText(context, "Bluetooth is turning off", Toast.LENGTH_SHORT).show();
                        Log.d("BroadcastActions", "Bluetooth is turning off");
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        Log.d("BroadcastActions", "Bluetooth is on");
                    }
                    break;

                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(context, "Connected to " + bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
                    Log.d("BroadcastActions", "Connected to " + bluetoothDevice.getName());

                    if (!new ServiceHelper(context).isTetheringWiFi()) {

                        List<BluetoothDevice> devicesToCheck = Utils.getBluetoothDevices(context, true);
                        for (BluetoothDevice device : devicesToCheck) {
                            if (device.getName() != null && device.getName().equals(bluetoothDevice.getName())) {
                                MyLog.i(TAG, "[Discovery] New connection to " + bluetoothDevice.getName());
                                Intent btIntent = new Intent(BT_CONNECTED);
                                btIntent.putExtra("name", bluetoothDevice.getName());
                                Utils.broadcast(context, btIntent);
                                break;
                            }
                        }
                    }
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(context, "Disconnected from " + bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();

                    Intent btIntent = new Intent(BT_DISCONNECTED);
                    btIntent.putExtra("name", bluetoothDevice.getName());
                    Utils.broadcast(context, btIntent);
                    //connectedDeviceName = null;
                    break;
            }
        }
    }
}
