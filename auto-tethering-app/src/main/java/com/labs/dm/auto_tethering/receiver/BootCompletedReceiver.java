package com.labs.dm.auto_tethering.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.service.TetheringService;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Main responsibility of this receiver is to start TetheringService instance just after boot has been completed
 * <p>
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final Intent serviceIntent = new Intent(context, TetheringService.class);
        int delay = Utils.strToInt(prefs.getString("activate.on.startup.delay", "0"));
        if (delay == 0) {
            if (SDK_INT < Build.VERSION_CODES.O) {
                context.startService(serviceIntent);
            } else {
                context.startForegroundService(serviceIntent);
            }
        } else {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent onPendingIntent = PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay * 1000L, onPendingIntent);
            } else if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay * 1000L, onPendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay * 1000L, onPendingIntent);
            }
        }
    }
}
