package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

            if (prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false)) {
                Intent serviceIntent = new Intent(context, TetheringService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
