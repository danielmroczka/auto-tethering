package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.labs.dm.auto_tethering.TetherIntents;

public class NetworkConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            Log.i("NCR", activeNetwork.getTypeName());
        }
        context.sendBroadcast(new Intent(TetherIntents.CHANGE_NETWORK_STATE));
    }
}
