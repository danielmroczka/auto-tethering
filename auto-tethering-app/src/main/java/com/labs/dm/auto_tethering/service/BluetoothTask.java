package com.labs.dm.auto_tethering.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;

import com.labs.dm.auto_tethering.Utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.labs.dm.auto_tethering.TetherIntents.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_DISCONNECTED;

/**
 * Class triggers only by MyBroadcastReceiver every xx seconds.
 * <p>
 * Created by Daniel Mroczka
 */
class BluetoothTask implements Runnable {

    private final ServiceHelper serviceHelper;
    private final Context context;
    private final SharedPreferences prefs;
    private final String TAG = "FindBT";
    private String connectedDeviceName;
    private final boolean initialBluetoothStatus;

    public BluetoothTask(Context context, SharedPreferences prefs, String connectedDeviceName, boolean initialBluetoothStatus) {
        this.serviceHelper = new ServiceHelper(context);
        this.context = context;
        this.prefs = prefs;
        this.connectedDeviceName = connectedDeviceName;
        this.initialBluetoothStatus = initialBluetoothStatus;
    }

    @Override
    public void run() {
        /**
         * Prepare a list with BluetoothDevice items
         */
        List<BluetoothDevice> devicesToCheck = getBluetoothDevices();
        Intent btIntent = null;

        if (devicesToCheck.isEmpty() && connectedDeviceName != null) {
            btIntent = new Intent(BT_DISCONNECTED);
            btIntent.putExtra("name", connectedDeviceName);
            Utils.broadcast(context, btIntent);
            connectedDeviceName = null;
        }

        if (!devicesToCheck.isEmpty()) {
            /**
             * Make sure that BT is enabled.
             */
            serviceHelper.setBlockingBluetoothStatus(true);
        }

        for (BluetoothDevice device : devicesToCheck) {
            /**
             * If device is currently connected just only check this one without checking others.
             */
            if (connectedDeviceName != null && !connectedDeviceName.equals(device.getName())) {
                continue;
            }

            try {
                connect(device);
                String previousConnectedDeviceName = connectedDeviceName;
                connectedDeviceName = device.getName();

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
                Utils.broadcast(context, btIntent);
                break;
            }
        }

        if (prefs.getBoolean("bt.internet.auto.off", false) && !initialBluetoothStatus) {
            serviceHelper.setBlockingBluetoothStatus(false);
        }
    }

    private List<BluetoothDevice> getBluetoothDevices() {
        List<BluetoothDevice> devicesToCheck = new ArrayList<>();
        List<String> preferredDevices = findPreferredDevices();
        for (BluetoothDevice device : serviceHelper.getBondedDevices()) {
            for (String pref : preferredDevices) {
                if (device.getName().equals(pref)) {
                    devicesToCheck.add(device);
                }
            }
        }
        return devicesToCheck;
    }

    private void connect(BluetoothDevice device) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = device.getClass().getMethod("getUuids");
        ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(parcelUuids[0].getUuid());
        Log.d(TAG, "Connecting to " + device.getName());
        try {
            socket.connect();
            Log.d(TAG, "Connected to " + device.getName());
        } catch (IOException e) {
            throw e;
        } finally {
            socket.close();
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

