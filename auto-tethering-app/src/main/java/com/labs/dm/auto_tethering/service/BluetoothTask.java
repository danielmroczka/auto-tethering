package com.labs.dm.auto_tethering.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import com.labs.dm.auto_tethering.Utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static com.labs.dm.auto_tethering.TetherIntents.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_DISCONNECTED;

/**
 * Class triggers only by MyBroadcastReceiver every xx seconds.
 * <p>
 * Created by Daniel Mroczka
 */
class BluetoothTask {
    private SharedPreferences prefs;
    private String TAG = "FindBT";
    private String connectedDeviceName;
    private boolean initialBluetoothStatus;
    private Context context;

    public BluetoothTask(Context context, SharedPreferences prefs, String connectedDeviceName, boolean initialBluetoothStatus) {
        this.context = context;
        this.context = context;
        this.prefs = prefs;
        this.connectedDeviceName = connectedDeviceName;
        this.initialBluetoothStatus = initialBluetoothStatus;
    }

    public void execute() {
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(new BluetoothThread(context, prefs, connectedDeviceName, initialBluetoothStatus));
    }

    class BluetoothThread implements Runnable {
        private final ServiceHelper serviceHelper;
        private final Context context;
        private final SharedPreferences prefs;
        private final String TAG = "FindBT";
        private String connectedDeviceName;
        private final boolean initialBluetoothStatus;

        public BluetoothThread(Context context, SharedPreferences prefs, String connectedDeviceName, boolean initialBluetoothStatus) {
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
            for (String pref : preferredDevices) {
                for (BluetoothDevice device : serviceHelper.getBondedDevices()) {
                    if (device.getName().equals(pref)) {
                        devicesToCheck.add(device);
                        break;
                    }
                }
            }
            return devicesToCheck;
        }

        private void connect(BluetoothDevice device) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Method method = device.getClass().getMethod("getUuids");
            ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            BluetoothSocket socket;
            if (Build.VERSION.SDK_INT <= JELLY_BEAN) {
                socket = device.createInsecureRfcommSocketToServiceRecord(parcelUuids[0].getUuid());
            } else {
                UUID uuid = parcelUuids.length >= 8 ? parcelUuids[7].getUuid() : parcelUuids[0].getUuid();
                socket = device.createRfcommSocketToServiceRecord(uuid);
            }

            Log.d(TAG, "Connecting to " + device.getName());
            boolean alreadyConnected = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && socket.isConnected()) {
                prefs.edit().putLong("bt.last.connect." + device.getName(), System.currentTimeMillis()).apply();
                alreadyConnected = true;
                Log.d(TAG, "Already connected to " + device.getName());
            }
            try {
                if (!alreadyConnected) {
                    prefs.edit().putLong("bt.last.connect." + device.getName(), System.currentTimeMillis()).apply();
                    socket.connect();
                    Log.d(TAG, "Connected to " + device.getName());
                }
            } catch (IOException e) {
                throw e;
            } finally {
                if (!alreadyConnected) {
                    socket.close();
                }
            }
        }

        /**
         * Returns the list of preferred devices ordered by the last time connection
         *
         * @return
         */
        private List<String> findPreferredDevices() {
            Map<String, ?> map = prefs.getAll();
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                if (entry.getKey().startsWith("bt.devices.")) {
                    list.add(String.valueOf(entry.getValue()));
                }
            }
            Collections.sort(list, new Comparator<String>() {
                @Override
                public int compare(String deviceName1, String deviceName2) {
                    long lastConnectTime1 = prefs.getLong("bt.last.connect." + deviceName1, 0);
                    long lastConnectTime2 = prefs.getLong("bt.last.connect." + deviceName2, 0);
                    return (int) (lastConnectTime2 - lastConnectTime1);
                }
            });
            return list;
        }
    }
}

