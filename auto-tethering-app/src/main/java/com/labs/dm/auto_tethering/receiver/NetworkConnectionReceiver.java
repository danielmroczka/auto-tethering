package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.labs.dm.auto_tethering.TetherIntents;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;

/**
 * Sends Intents once Mobile, WiFi of WiFi tethering state has changed
 * <p>
 * TetherIntents.EVENT_WIFI_ON
 * TetherIntents.EVENT_WIFI_OFF
 * TetherIntents.EVENT_MOBILE_ON
 * TetherIntents.EVENT_MOBILE_OFF
 * TetherIntents.EVENT_TETHER_ON - on tethering enabled (not triggers on enabling)
 * TetherIntents.EVENT_TETHER_OFF - on tethering disabled (not triggers on disabling)
 */
public class NetworkConnectionReceiver extends BroadcastReceiver {

    private final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentName = null;

        if (CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (null != activeNetwork) {
                boolean connected = activeNetwork.isConnected();
                if (activeNetwork.getType() == TYPE_WIFI) {
                    intentName = connected ? TetherIntents.EVENT_WIFI_ON : TetherIntents.EVENT_WIFI_OFF;
                } else if (activeNetwork.getType() == TYPE_MOBILE) {
                    intentName = connected ? TetherIntents.EVENT_MOBILE_ON : TetherIntents.EVENT_MOBILE_OFF;
                }
            }
        } else if (WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

            if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                intentName = TetherIntents.EVENT_TETHER_ON;
            } else if (WifiManager.WIFI_STATE_DISABLED == state % 10) {
                intentName = TetherIntents.EVENT_TETHER_OFF;
            }
        }

        if (intentName != null) {
            Log.i("NCR", intentName);
            context.sendBroadcast(new Intent(intentName));
        }
    }
}
