package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.format.DateUtils;

import com.labs.dm.auto_tethering.MyLog;
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
        long lastUpdate = prefs.getLong("data.usage.removeAllData.timestamp", 0);
        long lastBootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        /**
         * Execute for the very first time, set data with initial values
         */
        if (lastUpdate == 0) {
            MyLog.i(TAG, "Init data usage " + ServiceHelper.getDataUsage());
            reset(ServiceHelper.getDataUsage());
            lastUpdate = prefs.getLong("data.usage.removeAllData.timestamp", 0);
        }

        /**
         * Reset counter if 'reset counter every day' activated and this is the first execution on current day
         */
        if (prefs.getBoolean("data.limit.daily.reset", false) && !DateUtils.isToday(prefs.getLong("data.usage.removeAllData.timestamp", 0))) {
            MyLog.i(TAG, "Daily counter removeAllData" + ServiceHelper.getDataUsage());
            reset(ServiceHelper.getDataUsage());
        }

        /**
         * Restart device in the meantime, restore last stored value to counter
         */
        if (lastBootTime > lastUpdate) {
            MyLog.i(TAG, "Adjust after the boot " + ServiceHelper.getDataUsage());
            long offset = prefs.getLong("data.usage.last.value", 0) - Math.abs(prefs.getLong("data.usage.removeAllData.value", 0));
            prefs.edit().putLong("data.usage.removeAllData.value", -offset).apply();
        }

        prefs.edit().putLong("data.usage.last.value", ServiceHelper.getDataUsage()).apply();
        long usage = ServiceHelper.getDataUsage() - prefs.getLong("data.usage.removeAllData.value", 0);
        Intent onIntent = new Intent(TetherIntents.DATA_USAGE);
        onIntent.putExtra("value", usage);
        context.sendBroadcast(onIntent);
    }

    private void reset(long value) {
        prefs.edit().putLong("data.usage.removeAllData.value", value).apply();
        prefs.edit().putLong("data.usage.last.value", value).apply();
        prefs.edit().putLong("data.usage.removeAllData.timestamp", System.currentTimeMillis()).apply();

    }
}
