package com.labs.dm.auto_tethering.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.TimerTask;

/**
 * Created by Daniel Mroczka on 6/3/2016.
 */
public class BluetoothTimerTask extends TimerTask {
    private Context context;
    private SharedPreferences prefs;

    public BluetoothTimerTask(Context context, SharedPreferences prefs) {
        Log.i("BT Constructor", "ping");
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
    }
}

