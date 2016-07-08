package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.TimerTask;

import static com.labs.dm.auto_tethering.TetherInvent.BT_SEARCH;

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
        Intent checkBtDevice = new Intent(BT_SEARCH);
        context.sendBroadcast(checkBtDevice);
    }

}

