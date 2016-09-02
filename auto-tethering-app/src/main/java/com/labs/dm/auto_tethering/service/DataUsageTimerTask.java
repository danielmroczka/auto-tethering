package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import com.labs.dm.auto_tethering.TetherIntents;

import java.util.TimerTask;

/**
 * Created by Daniel Mroczka on 6/3/2016.
 */
public class DataUsageTimerTask extends TimerTask {
    private static final String TAG = "DataUsageTimerTask";
    private final Context context;
    private final SharedPreferences prefs;

    public DataUsageTimerTask(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    @Override
    public void run() {
        long lastUpdate = prefs.getLong("data.usage.reset.timestamp", 0);
        long lastBootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        /**
         * Execute for the very first time, set data with initial values
         */
        if (lastUpdate == 0) {
            Log.i(TAG, "Init data usage " + ServiceHelper.getDataUsage());
            prefs.edit().putLong("data.usage.reset.value", ServiceHelper.getDataUsage()).apply();
            prefs.edit().putLong("data.usage.last.value", ServiceHelper.getDataUsage()).apply();
            prefs.edit().putLong("data.usage.reset.timestamp", System.currentTimeMillis()).apply();
            lastUpdate = prefs.getLong("data.usage.reset.timestamp", 0);
        }
        if (prefs.getBoolean("data.limit.daily.reset", false) && !DateUtils.isToday(prefs.getLong("data.usage.reset.timestamp", 0))) {
            Log.i(TAG, "Daily counter reset" + ServiceHelper.getDataUsage());
            long dataUsage = ServiceHelper.getDataUsage();
            prefs.edit().putLong("data.usage.reset.value", dataUsage).apply();
            prefs.edit().putLong("data.usage.last.value", dataUsage).apply();
            prefs.edit().putLong("data.usage.reset.timestamp", System.currentTimeMillis()).apply();
        }

        /**
         * Restart device in the meantime, restore last stored value to counter
         */
        if (lastBootTime > lastUpdate) {
            Log.i(TAG, "Adjust after the boot " + ServiceHelper.getDataUsage());
            long offset = prefs.getLong("data.usage.last.value", 0) - Math.abs(prefs.getLong("data.usage.reset.value", 0));
            prefs.edit().putLong("data.usage.reset.value", -offset).apply();
            prefs.edit().putLong("data.usage.reset.timestamp", System.currentTimeMillis()).apply();
        }

        prefs.edit().putLong("data.usage.last.value", ServiceHelper.getDataUsage()).apply();
        long usage = ServiceHelper.getDataUsage() - prefs.getLong("data.usage.reset.value", 0);
        Intent onIntent = new Intent(TetherIntents.DATA_USAGE);
        onIntent.putExtra("value", usage);
        context.sendBroadcast(onIntent);
    }
}
