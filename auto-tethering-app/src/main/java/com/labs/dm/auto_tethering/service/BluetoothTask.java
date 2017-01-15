package com.labs.dm.auto_tethering.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Bluetooth;
import com.labs.dm.auto_tethering.db.DBManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.labs.dm.auto_tethering.TetherIntents.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_DISCONNECTED;

/**
 * Class triggers by BluetoothTimerTask every xx seconds responsible for pooling all configured Bluetooth devices if they in range.
 * If any of device is in the range it triggers Intent BT_CONNECTED
 * Intent BT_CONNECTED will be triggered if already connected devices disconnects.
 * <p>
 * <p>
 * Created by Daniel Mroczka
 */
class BluetoothTask {
    private final String TAG = "FindBT";
    private final String connectedDeviceName;
    private final Context context;

    public BluetoothTask(Context context, String connectedDeviceName) {
        this.context = context;
        this.connectedDeviceName = connectedDeviceName;
    }

    public void execute() {
        new Thread(new BluetoothThread(context, connectedDeviceName)).start();
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
            List<BluetoothDevice> devicesToCheck = Utils.getBluetoothDevices(context, true);

            if (devicesToCheck.isEmpty() && connectedDeviceName != null) {
                Intent btIntent = new Intent(BT_DISCONNECTED);
                btIntent.putExtra("name", connectedDeviceName);
                Utils.broadcast(context, btIntent);
                connectedDeviceName = null;
                return;
            }

            if (!devicesToCheck.isEmpty() && !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                /**
                 * Make sure that BT is enabled.
                 */
                serviceHelper.setBluetoothStatus(true);
            }

            connectEachDevice(devicesToCheck);
        }

        private void connectEachDevice(List<BluetoothDevice> devicesToCheck) {
            Intent btIntent = null;
            checkIfConnectedDeviceRemoved(devicesToCheck);

            MyLog.d(TAG, "Start interrupting " + devicesToCheck.size() + " devices");

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
                    MyLog.e(TAG, "[" + device.getName() + "] is not in range. " + e.getMessage());
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

            if (connectedDeviceName == null) {
                serviceHelper.setBlockingBluetoothStatus(false);
                serviceHelper.setBlockingBluetoothStatus(true);
            }
        }

        private void checkIfConnectedDeviceRemoved(List<BluetoothDevice> devicesToCheck) {
            boolean found = false;

            for (BluetoothDevice device : devicesToCheck) {
                if (connectedDeviceName != null && connectedDeviceName.equals(device.getName())) {
                    found = true;
                    break;
                }
            }
            if (connectedDeviceName != null && !found) {
                Intent btIntent = new Intent(BT_DISCONNECTED);
                btIntent.putExtra("name", connectedDeviceName);
                Utils.broadcast(context, btIntent);
                connectedDeviceName = null;
            }
        }

        private void connect(BluetoothDevice device) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Method method = device.getClass().getMethod("getUuids");
            ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            final BluetoothSocket socket;

            MyLog.d(TAG, "Connecting to " + device.getName());

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            int parcelId = getParcelId(device);
            if (parcelUuids != null && parcelUuids.length > 0) {

                if (parcelId >= 0 && parcelUuids.length > parcelId) {
                    uuid = parcelUuids[parcelId].getUuid();
                    MyLog.d(TAG, "Using uuid " + parcelId + "/" + (parcelUuids.length - 1) + " " + parcelUuids[parcelId].getUuid());
                } else {
                    Random random = new Random();
                    parcelId = random.nextInt(parcelUuids.length);
                    uuid = parcelUuids[parcelId].getUuid();
                    MyLog.d(TAG, "Selecting uuid " + parcelId + "/" + (parcelUuids.length - 1) + " " + parcelUuids[parcelId].getUuid());
                }
            }

            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean alreadyConnected = false;
            if (Build.VERSION.SDK_INT >= ICE_CREAM_SANDWICH && socket.isConnected()) {
                updateTimestamp(device, parcelId);
                alreadyConnected = true;
                MyLog.d(TAG, "Already connected to " + device.getName());
            }
            if (!alreadyConnected) {
                try {
                    socket.connect();
                    updateTimestamp(device, parcelId);
                    MyLog.d(TAG, "Connected to " + device.getName());
                } finally {
                    close(socket);
                }
            }
        }

        private void close(BluetoothSocket socket) {
            if (socket != null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                    socket.getInputStream().close();
                    socket.getOutputStream().close();
                    socket.close();
                } catch (IOException e) {
                    MyLog.e(TAG, "failed closing I/O streams");
                } catch (InterruptedException e) {
                    MyLog.e(TAG, "failed closing I/O streams");
                }
            }
        }

        private int getParcelId(BluetoothDevice device) {
            for (Bluetooth b : DBManager.getInstance(context).readBluetooth()) {
                if (device.getName().equals(b.getName())) {
                    return b.getParcelId();
                }
            }
            return -1;
        }

        private void updateTimestamp(BluetoothDevice device, int parcelId) {
            for (Bluetooth b : DBManager.getInstance(context).readBluetooth()) {
                if (device.getName().equals(b.getName())) {
                    b.setUsed(System.currentTimeMillis());
                    b.setParcelId(parcelId);
                    DBManager.getInstance(context).addOrUpdateBluetooth(b);
                }
            }
        }
    }
}

