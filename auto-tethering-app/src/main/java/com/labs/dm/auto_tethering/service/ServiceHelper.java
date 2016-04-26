package com.labs.dm.auto_tethering.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper class responsible for communication with WIFI and mobile services
 * <p/>
 * Created by Daniel Mroczka on 2015-10-26.
 */
public class ServiceHelper {

    private final Context context;
    private final WifiManager wifiManager;
    private final String TAG = "ServiceHelper";

    public ServiceHelper(Context context) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.context = context;
    }

    /**
     * Returns true if currently Wi-Fi tethering is enabled.
     *
     * @return
     */
    public boolean isSharingWiFi() {
        try {
            final Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (IllegalAccessException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (InvocationTargetException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, ex.getMessage());
        }

        return false;
    }

    /**
     * Returns declared portable Wi-Fi hotspot network SSID.
     *
     * @return network SSID
     */
    public String getTetheringSSID() {
        WifiConfiguration cfg = getWifiApConfiguration(context);
        return (cfg != null ? cfg.SSID : "");
    }

    /**
     * Returns true if internet connection provided by mobile is currently active.
     *
     * @return
     */
    public boolean isMobileConnectionActive() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
    }

    /**
     * Changing Wifi Tethering state
     *
     * @param enable
     */
    public void setWifiTethering(boolean enable) {
        wifiManager.setWifiEnabled(false);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    Log.i(TAG, "setWifiTethering to " + enable);
                    method.invoke(wifiManager, null, enable);
                } catch (Exception ex) {
                    Log.e(TAG, "Switch on tethering", ex);
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    /**
     * Connecting to internet through mobile phone
     *
     * @param enabled
     */
    public void setMobileDataEnabled(boolean enabled) {
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
            Log.e(TAG, "Changing mobile connection state", e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private WifiConfiguration getWifiApConfiguration(final Context ctx) {
        final WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        final Method m = getWifiManagerMethod("getWifiApConfiguration", wifiManager);
        if (m != null) {
            try {
                return (WifiConfiguration) m.invoke(wifiManager);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return null;
    }

    private Method getWifiManagerMethod(final String methodName, final WifiManager wifiManager) {
        final Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}
