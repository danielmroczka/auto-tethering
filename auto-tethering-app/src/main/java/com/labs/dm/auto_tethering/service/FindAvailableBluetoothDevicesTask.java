package com.labs.dm.auto_tethering.service;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.labs.dm.auto_tethering.TetherInvent.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherInvent.BT_DISCONNECTED;

/**
 * Class triggers only by MyBroadcastReceiver every xx seconds.
 *
 * Created by Daniel Mroczka
 */
class FindAvailableBluetoothDevicesTask implements Runnable {

    private final ServiceHelper serviceHelper;
    private final Context context;
    private final SharedPreferences prefs;
    private final String TAG = "FindBT";
    private String connectedDeviceName;

    public FindAvailableBluetoothDevicesTask(Context context, SharedPreferences prefs, String connectedDeviceName) {
        this.serviceHelper = new ServiceHelper(context);
        this.context = context;
        this.prefs = prefs;
        this.connectedDeviceName = connectedDeviceName;
    }

    @Override
    public void run() {
        /**
         * Make sure that BT is enabled.
         */
        serviceHelper.setBlockingBluetoothStatus(true);

        /**
         * Prepare a list with BluetoothDevice items
         */
        List<BluetoothDevice> devicesToCheck = new ArrayList<>();
        List<String> preferredDevices = findPreferredDevices();
        for (BluetoothDevice device : serviceHelper.getBondedDevices()) {
            for (String pref : preferredDevices) {
                if (device.getName().equals(pref)) {
                    devicesToCheck.add(device);
                }
            }
        }

        for (BluetoothDevice device : devicesToCheck) {
            /**
             * If device is currently connected just only check this one without checking others.
             */
            if (connectedDeviceName != null && !connectedDeviceName.equals(device.getName())) {
                continue;
            }

            Intent btIntent = null;
            try {
                Method method = device.getClass().getMethod("getUuids");
                method.setAccessible(true);
                ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
                BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(parcelUuids[0].getUuid());
                Log.d(TAG, "Connecting to " + device.getName());
                socket.connect();
                Log.d(TAG, "Connected to " + device.getName());
                String previousConnectedDeviceName = connectedDeviceName;
                connectedDeviceName = device.getName();
                socket.close();

                if (connectedDeviceName != null) {
                    if (previousConnectedDeviceName == null || !connectedDeviceName.equals(previousConnectedDeviceName)) {
                        Log.i(TAG, "Connected to " + device.getName());
                        btIntent = new Intent(BT_CONNECTED);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, device.getName() + " Device is not in range.");
                if (connectedDeviceName != null && connectedDeviceName.equals(device.getName())) {
                    Log.i(TAG, device.getName() + " device has been disconnected");
                    btIntent = new Intent(BT_DISCONNECTED);
                }
                connectedDeviceName = null;
            }

            if (btIntent != null) {
                btIntent.putExtra("name", device.getName());
                PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    onPendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }
                break;
            }
        }

        if (prefs.getBoolean("bt.internet.auto.off", false)) {
            serviceHelper.setBlockingBluetoothStatus(false);
        }
    }

    private List<String> findPreferredDevices() {
        Map<String, ?> map = prefs.getAll();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().startsWith("bt.devices.")) {
                list.add(String.valueOf(entry.getValue()));
            }
        }
        return list;
    }
}
