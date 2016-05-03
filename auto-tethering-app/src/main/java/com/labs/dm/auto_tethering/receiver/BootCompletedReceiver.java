package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Main responsibility of this receiver is to start TetheringService instance just after boot has been completed
 *
 * Created by Daniel Mroczka
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, TetheringService.class);
        context.startService(serviceIntent);
    }
}
