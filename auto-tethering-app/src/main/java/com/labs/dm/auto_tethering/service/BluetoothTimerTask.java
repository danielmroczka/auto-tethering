package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.TimerTask;

import static com.labs.dm.auto_tethering.TetherIntents.BT_SEARCH;

/**
 * Created by Daniel Mroczka on 6/3/2016.
 */
public class BluetoothTimerTask extends TimerTask {

    private final Context context;

    private final SharedPreferences prefs;

    public BluetoothTimerTask(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    @Override
    public void run() {
        if (prefs.getBoolean("bt.start.discovery", false)) {
            context.sendBroadcast(new Intent(BT_SEARCH));
        }
    }

}

