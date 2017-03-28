package com.labs.dm.auto_tethering.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Bluetooth;
import com.labs.dm.auto_tethering.db.DBManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
        new Thread(new DiscoveryThread(connectedDeviceName)).start();

    }

    private class DiscoveryThread implements Runnable {
        private String connectedDeviceName;

        public DiscoveryThread(String connectedDeviceName) {
            this.connectedDeviceName = connectedDeviceName;
        }

        @Override
        public void run() {
            startDiscovery();
        }

        private void startDiscovery() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

            if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
            context.registerReceiver(btReceiver, filter);
        }

        private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

            private List<BluetoothDevice> devices = new ArrayList<>();

            @Override
            public void onReceive(Context context, Intent intent) {

                switch (intent.getAction()) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        MyLog.d(TAG, "BT discovery started");
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device.getName() != null) {
                            devices.add(device);
                        }
                        MyLog.d(TAG, "Found: " + device.getName() + " " + device.getAddress());
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        MyLog.d(TAG, "Finished discovery, found devices: " + devices.size());
                        context.unregisterReceiver(this);
                        new Thread(new BluetoothThread(connectedDeviceName, devices)).start();
                        break;
                }
            }
        };
    }

    private class BluetoothThread implements Runnable {
        private final ServiceHelper serviceHelper;
        private String connectedDeviceName;
        private List<BluetoothDevice> discoveredDevices;

        public BluetoothThread(String connectedDeviceName, List<BluetoothDevice> discoveredDevices) {
            this.serviceHelper = new ServiceHelper(context);
            this.connectedDeviceName = connectedDeviceName;
            this.discoveredDevices = discoveredDevices;
        }

        @Override
        public void run() {
            startConnect(discoveredDevices);
        }


        private void startConnect(List<BluetoothDevice> discoveredDeveices) {
            /* Prepare a list with BluetoothDevice items */
            List<BluetoothDevice> devicesToCheck = Utils.getBluetoothDevices(context, true);

            for (BluetoothDevice discoveredDevice : discoveredDeveices) {
                for (BluetoothDevice device : devicesToCheck) {
                    if (device.getName() != null && device.getName().equals(discoveredDevice.getName())) {
                        String previousConnectedDeviceName = connectedDeviceName;
                        connectedDeviceName = device.getName();
                        if (connectedDeviceName != null && (previousConnectedDeviceName == null || !connectedDeviceName.equals(previousConnectedDeviceName))) {
                            MyLog.i(TAG, "[Discovery] New connection to " + device.getName());
                            Intent btIntent = new Intent(BT_CONNECTED);
                            btIntent.putExtra("name", device.getName());
                            Utils.broadcast(context, btIntent);
                            return;
                        }
                    }
                }
            }

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
                 * If device is currently connected just only check this one without checking others to improve performance
                 */
                if (connectedDeviceName != null && !connectedDeviceName.equals(device.getName())) {
                    continue;
                }

                try {

                    connect(device);
                    String previousConnectedDeviceName = connectedDeviceName;
                    connectedDeviceName = device.getName();

                    if (connectedDeviceName != null && (previousConnectedDeviceName == null || !connectedDeviceName.equals(previousConnectedDeviceName))) {
                        MyLog.i(TAG, "New connection to " + device.getName());
                        btIntent = new Intent(BT_CONNECTED);
                    } else if (connectedDeviceName != null && !serviceHelper.isTetheringWiFi()) {
                        MyLog.i(TAG, "Restore connection to " + device.getName());
                        btIntent = new Intent(BT_CONNECTED);
                    }

                } catch (Exception ex) {
                    MyLog.e(TAG, "[" + device.getName() + "] is not in range. " + (ex != null ? ex.getMessage() : ""));
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
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            Method method = device.getClass().getMethod("getUuids");
            ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
            final BluetoothSocket socket;

            MyLog.d(TAG, "Connecting to " + device.getName());

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            int parcelIndex = -1;
            if (parcelUuids != null && parcelUuids.length > 0) {

                parcelIndex = getParcelIndex(device);
                if (parcelIndex >= 0 && parcelUuids.length > parcelIndex) {
                    MyLog.d(TAG, "Using uuid " + parcelIndex + "/" + parcelUuids.length + " " + parcelUuids[parcelIndex].getUuid());
                } else {
                    // As a default try connect to SPP
                    for (int id = 0; id < parcelUuids.length; id++) {
                        if (parcelUuids[id].toString().startsWith("00001101")) {
                            parcelIndex = id;
                            break;
                        }
                    }
                    // If SPP is not present on the list try to connect with any other uuid
                    if (parcelIndex < 0) {
                        do {
                            Random random = new Random();
                            parcelIndex = random.nextInt(parcelUuids.length);
                        }
                        while (parcelUuids.length > 3 &&
                                (parcelUuids[parcelIndex].getUuid().toString().startsWith("0000112f") ||
                                        parcelUuids[parcelIndex].getUuid().toString().startsWith("0000112d") ||
                                        parcelUuids[parcelIndex].getUuid().toString().startsWith("00001132")));
                    }

                    MyLog.d(TAG, "Selecting uuid " + parcelIndex + "/" + parcelUuids.length + " " + parcelUuids[parcelIndex].getUuid());
                }
                uuid = parcelUuids[parcelIndex].getUuid();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } else {
                socket = device.createRfcommSocketToServiceRecord(uuid);
            }

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean alreadyConnected = false;
            if (Build.VERSION.SDK_INT >= ICE_CREAM_SANDWICH && socket.isConnected()) {
                updateTimestamp(device, parcelIndex);
                alreadyConnected = true;
                MyLog.d(TAG, "Already connected to " + device.getName());
            }
            if (!alreadyConnected) {
                try {
                    socket.connect();
                    updateTimestamp(device, parcelIndex);
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

        private int getParcelIndex(BluetoothDevice device) {
            for (Bluetooth bluetooth : DBManager.getInstance(context).readBluetooth()) {
                if (device.getName() != null && device.getName().equals(bluetooth.getName())) {
                    return bluetooth.getParcelId();
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

        private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

            private List<BluetoothDevice> devices = new ArrayList<>();

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device.getName() != null) {
                            devices.add(device);
                        }
                        MyLog.i(TAG, "Found: " + device.getName() + " " + device.getAddress());
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        MyLog.i(TAG, "Finished: " + devices.size());
                        context.unregisterReceiver(this);
                        startConnect(devices);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        MyLog.i(TAG, "Started: ");
                        break;
                }
            }
        };
    }
}

