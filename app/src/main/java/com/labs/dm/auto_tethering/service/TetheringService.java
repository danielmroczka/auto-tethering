package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.labs.dm.auto_tethering.AppProperties;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TetheringService extends IntentService {

    private static final String TAG = "MyTetheringService";
    private AppProperties props;
    private boolean state;
    private Calendar timeOff;
    private Calendar timeOn;
    private SharedPreferences prefs;

    public TetheringService() {
        super("TetheringService");
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public static WifiConfiguration getWifiApConfiguration(final Context ctx) {
        final WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        final Method m = getWifiManagerMethod("getWifiApConfiguration", wifiManager);
        if (m != null) {
            try {
                return (WifiConfiguration) m.invoke(wifiManager);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static Method getWifiManagerMethod(final String methodName, final WifiManager wifiManager) {
        final Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Check if there is any connectivity
     *
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        timeOff = Calendar.getInstance();
        timeOn = Calendar.getInstance();
        try {
            timeOff.setTime(formatter.parse(prefs.getString(AppProperties.TIME_OFF, "")));
            timeOn.setTime(formatter.parse(prefs.getString(AppProperties.TIME_ON, "")));

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void switcher(boolean state) {
        this.state = state;
        if (prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false) && isCorrectSimCard()) {
            Log.i(TAG, "Start working...");
            if (prefs.getBoolean(AppProperties.ACTIVATE_3G, false)) {
                setMobileDataEnabled(getApplicationContext(), !state);
                setMobileDataEnabled(getApplicationContext(), state);
            }
            if (prefs.getBoolean(AppProperties.ACTIVATE_TETHERING, false)) {
                setWifiTetheringEnabled(state);
            }
        }

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switcher(true);

        while (true) {
            onTick();
        }
    }

    private void onTick() {
        Calendar c = Calendar.getInstance();
        timeOn.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        timeOff.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        try {
            if (c.after(timeOff) && c.before(timeOn)) {
                if (state) {
                    switcher(false);
                }
            } else {
                if (!state) {
                    switcher(true);
                }
            }

            if (prefs.getBoolean(AppProperties.ACTIVATE_TETHERING, false) && !isSharingWiFi()) {
                setWifiTetheringEnabled(true);
            }

            if (prefs.getBoolean(AppProperties.ACTIVATE_3G, false) && !isConnected(getApplicationContext())) {
                setMobileDataEnabled(getApplicationContext(), false);
                setMobileDataEnabled(getApplicationContext(), true);
            }

            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    public boolean isCorrectSimCard() {
        if (!prefs.getString(AppProperties.SIMCARD, "").isEmpty()) {
            TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String simCard = tMgr.getSimSerialNumber();
            return prefs.getString(AppProperties.SIMCARD, "").equals(simCard);
        } else {
            return true;
        }
    }
}
