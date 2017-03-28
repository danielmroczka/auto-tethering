package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;

import java.util.TimerTask;

/**
 * Created by Daniel Mroczka on 6/3/2016.
 */
class DataUsageTimerTask extends TimerTask {
    private static final String TAG = "DataUsageTimerTask";
    private final Context context;
    private final SharedPreferences prefs;

    public DataUsageTimerTask(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void run() {
        long lastReset = prefs.getLong("data.usage.removeAllData.timestamp", 0);
        long lastUpdate = prefs.getLong("data.usage.update.timestamp", 0);
        long lastBootTime = lastBootTime();

        /**
         * Execute for the very first time (new installation), set data with initial values
         */
        if (lastReset == 0) {
            MyLog.i(TAG, "Init data usage " + ServiceHelper.getDataUsage());
            reset(ServiceHelper.getDataUsage());
        }

        /**
         * Reset counter if 'reset counter every day' activated and this is the first execution after midnight
         */
        if (prefs.getBoolean("data.limit.daily.reset", false) && !DateUtils.isToday(prefs.getLong("data.usage.removeAllData.timestamp", 0))) {
            MyLog.i(TAG, "Daily counter removeAllData" + ServiceHelper.getDataUsage());
            reset(ServiceHelper.getDataUsage());
        }

        /**
         * Restart device after last counter reset, restore last stored value to counter
         */
        if (lastBootTime > lastUpdate) {
            MyLog.i(TAG, "Adjust after the boot " + ServiceHelper.getDataUsage());
            long offset = prefs.getLong("data.usage.last.value", 0) + prefs.getLong("data.usage.removeAllData.value", 0);
            prefs.edit().putLong("data.usage.removeAllData.value", offset).apply();
            prefs.edit().putLong("data.usage.update.timestamp", System.currentTimeMillis()).apply();
        }

        prefs.edit().putLong("data.usage.last.value", ServiceHelper.getDataUsage()).apply();
        //MyLog.d("datausage" , ServiceHelper.getDataUsage() + " | " + prefs.getLong("data.usage.removeAllData.value", 0) + " | " + prefs.getLong("data.usage.last.value", 0));
        long usage = ServiceHelper.getDataUsage() + prefs.getLong("data.usage.removeAllData.value", 0);
        Intent onIntent = new Intent(TetherIntents.DATA_USAGE);
        onIntent.putExtra("value", usage);
        context.sendBroadcast(onIntent);
    }

    private void reset(long value) {
        Utils.resetDataUsageStat(prefs, -value, 0);
    }

    private long lastBootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }
}
