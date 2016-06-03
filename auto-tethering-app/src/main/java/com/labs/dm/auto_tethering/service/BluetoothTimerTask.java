package com.labs.dm.auto_tethering.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

/**
 * Created by Daniel Mroczka on 6/3/2016.
 */
public class BluetoothTimerTask extends TimerTask {
    private Context context;
    private SharedPreferences prefs;
    private List<String> devices = new ArrayList<>();

    public BluetoothTimerTask(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    @Override
    public void run() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (prefs.getBoolean("bt.start.discovery", false)) {

            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }

            if (!mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.startDiscovery();
            }
        }
        boolean found = false;
        for (BluetoothDevice dev : mBluetoothAdapter.getBondedDevices()) {
            for (String name : devices) {
                if (dev.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
        }

        if (found) {
            Log.i(TAG, "Found paired bt in range!");
        }
    }

}

