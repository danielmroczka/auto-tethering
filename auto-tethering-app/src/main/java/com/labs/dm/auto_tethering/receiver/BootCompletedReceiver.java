package com.labs.dm.auto_tethering.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Main responsibility of this receiver is to start TetheringService instance just after boot has been completed
 * <p>
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean("usb.only.when.connected", false)) {
            ServiceHelper serviceHelper = new ServiceHelper(context);
            if (!serviceHelper.isPluggedToPower()) {
                MyLog.d("Boot", "Service is not triggered due to USB configuration");
                //return; TODO
            }
        }

        final Intent serviceIntent = new Intent(context, TetheringService.class);
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
