package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;

import java.util.TimerTask;

import static com.labs.dm.auto_tethering.TetherIntents.BT_START_SEARCH;

/**
 * Created by Daniel Mroczka on 6/3/2016.
 */
public class BluetoothTimerTask extends TimerTask {

    private final Context context;

    public BluetoothTimerTask(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        context.sendBroadcast(new Intent(BT_START_SEARCH));
    }

}

