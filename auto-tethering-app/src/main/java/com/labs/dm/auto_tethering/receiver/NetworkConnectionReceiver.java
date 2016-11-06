package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.labs.dm.auto_tethering.TetherIntents;

public class NetworkConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent(TetherIntents.CHANGE_NETWORK_STATE));
    }
}
