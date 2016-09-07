package com.labs.dm.auto_tethering.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import com.labs.dm.auto_tethering.TetherIntents;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lrxmrod on 9/7/2016.
 */
public class WiFiReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context c, Intent intent) {
        if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            WifiManager wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> mScanResults = wifi.getScanResults();
            Intent i = new Intent(TetherIntents.WIFI_SCAN_RESULTS);

            ArrayList<String> list = new ArrayList<>();
            for (ScanResult res : mScanResults) {
                list.add(res.SSID);

                i.putStringArrayListExtra("list", list);
                c.sendBroadcast(i);
            }
        }
    }
}
