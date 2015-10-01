package com.labs.dm.auto_tethering;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TetheringService extends IntentService {

    private static final String TAG = "MyTetheringService";
    private AppProperties props;
    private boolean state;

    public TetheringService() {
        super("TetheringService");
        props = new AppProperties();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        props.load(getBaseContext());
    }

    private void switcher(boolean state) {
        this.state = state;
        if (props.isActivateOnStartup()) {
            Log.i(TAG, "Start working...");
            if (props.isActivate3G()) {
                setMobileDataEnabled(getApplicationContext(), !state);
                setMobileDataEnabled(getApplicationContext(), state);
            }
            if (props.isActivateTethering()) {
                setWifiTetheringEnabled(state);
            }
        }

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switcher(true);

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(15);
                Calendar c = Calendar.getInstance();

                Calendar timeOff = Calendar.getInstance();
                timeOff.set(Calendar.HOUR, 0);
                timeOff.set(Calendar.MINUTE, 25);
                timeOff.set(Calendar.SECOND, 0);
                Calendar timeOn = Calendar.getInstance();
                timeOn.set(Calendar.HOUR, 0);
                timeOn.set(Calendar.MINUTE, 28);
                timeOn.set(Calendar.SECOND, 0);

                if (c.after(timeOff) && c.before(timeOn)) {
                    if (state) {
                        switcher(false);
                    }
                } else {
                    if (!state) {
                        switcher(true);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setWifiTetheringEnabled(boolean enable) {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    Log.i(TAG, "switching on tethering...");

//                    WifiConfiguration netConfig = new WifiConfiguration();
//                    netConfig.SSID = "\"PROVAAP\"";
//                    netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//                    netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//                    netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//                    netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//                    netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//                    netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//                    netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//                    netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                    method.invoke(wifiManager, null, enable);
                } catch (Exception ex) {
                    Log.e(TAG, "Switch on tethering", ex);
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG);
                }
                break;
            }
        }
    }

    private void setMobileDataEnabled(Context context, boolean enabled) {
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e) {
            Log.e(TAG, "Switch on 3G", e);
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private boolean isSharingWiFi() {
        try {
            WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
            final Method method = manager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(manager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return false;
    }


}
