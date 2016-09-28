package com.labs.dm.auto_tethering.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Main responsibility of this receiver is to start TetheringService instance just after boot has been completed
 * <p>
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final Intent serviceIntent = new Intent(context, TetheringService.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int delay = Integer.parseInt(prefs.getString("activate.on.startup.delay", "0"));
        if (delay == 0) {
            context.startService(serviceIntent);
        } else {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent onPendingIntent = PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay * 1000L, onPendingIntent);
        }
    }
}
