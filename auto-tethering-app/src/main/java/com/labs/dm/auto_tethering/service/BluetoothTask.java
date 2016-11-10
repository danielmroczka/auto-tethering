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

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Bluetooth;
import com.labs.dm.auto_tethering.db.DBManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static com.labs.dm.auto_tethering.TetherIntents.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_DISCONNECTED;

/**
 * Class triggers only by MyBroadcastReceiver every xx seconds.
 * <p>
 * Created by Daniel Mroczka
 */
class BluetoothTask {
    private final SharedPreferences prefs;
    private String TAG = "FindBT";
    private final String connectedDeviceName;
    private Context context;

    public BluetoothTask(Context context, SharedPreferences prefs, String connectedDeviceName) {
        this.context = context;
        this.context = context;
        this.prefs = prefs;
        this.connectedDeviceName = connectedDeviceName;
    }

    public void execute() {
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(new BluetoothThread(context, connectedDeviceName));
    }

    private class BluetoothThread implements Runnable {
        private final ServiceHelper serviceHelper;
        private final Context context;
        private String connectedDeviceName;

        public BluetoothThread(Context context, String connectedDeviceName) {
            this.serviceHelper = new ServiceHelper(context);
            this.context = context;
            this.connectedDeviceName = connectedDeviceName;
        }

        @Override
        public void run() {
            /**
             * Prepare a list with BluetoothDevice items
             */
            List<BluetoothDevice> devicesToCheck = Utils.getBluetoothDevices(context);
            Intent btIntent = null;

            if (devicesToCheck.isEmpty() && connectedDeviceName != null) {
                btIntent = new Intent(BT_DISCONNECTED);
                btIntent.putExtra("name", connectedDeviceName);
                Utils.broadcast(context, btIntent);
                connectedDeviceName = null;
            }

            if (!devicesToCheck.isEmpty() && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                /**
                 * Make sure that BT is enabled.
                 */
                serviceHelper.setBlockingBluetoothStatus(true);
            }

            connectEachDevice(devicesToCheck, btIntent);
        }

        private void connectEachDevice(List<BluetoothDevice> devicesToCheck, Intent btIntent) {
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

                    if (connectedDeviceName != null && (previousConnectedDeviceName == null || !connectedDeviceName.equals(previousConnectedDeviceName))) {
                        MyLog.i(TAG, "Connected to " + device.getName());
                        btIntent = new Intent(BT_CONNECTED);
                    }

                } catch (Exception e) {
                    MyLog.e(TAG, device.getName() + " Device is not in range.");
                    if (connectedDeviceName != null && connectedDeviceName.equals(device.getName())) {
                        MyLog.i(TAG, device.getName() + " device has been disconnected");
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
        }

        private void connect(BluetoothDevice device) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Method method = device.getClass().getMethod("getUuids");
            ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            BluetoothSocket socket;

            MyLog.d(TAG, "Connecting to " + device.getName());

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            if (Build.VERSION.SDK_INT <= JELLY_BEAN) {
                if (parcelUuids != null && parcelUuids.length > 0) {
                    uuid = parcelUuids[0].getUuid();
                }
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } else {
                if (parcelUuids != null && parcelUuids.length > 0) {
                    uuid = parcelUuids.length >= 8 ? parcelUuids[7].getUuid() : parcelUuids[0].getUuid();
                }
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            }

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean alreadyConnected = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && socket.isConnected()) {
                updateTimestamp(device);
                alreadyConnected = true;
                MyLog.d(TAG, "Already connected to " + device.getName());
            }
            if (!alreadyConnected) {
                try {
                    socket.connect();
                    updateTimestamp(device);
                    MyLog.d(TAG, "Connected to " + device.getName());
                } finally {

                    if (socket != null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                            socket.getInputStream().close();
                            socket.getOutputStream().close();
                            socket.close();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }

        private void updateTimestamp(BluetoothDevice device) {
            for (Bluetooth b : DBManager.getInstance(context).readBluetooth()) {
                if (device.getName().equals(b.getName())) {
                    b.setUsed(System.currentTimeMillis());
                    DBManager.getInstance(context).addOrUpdateBluetooth(b);
                }
            }
        }
    }
}

