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

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private static final String TAG = "MyTetheringService";
    private Calendar timeOff;
    private Calendar timeOn;
    private SharedPreferences prefs;

    public TetheringService() {
        super("TetheringService");
    }

    public static WifiConfiguration getWifiApConfiguration(final Context ctx) {
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

    private static Method getWifiManagerMethod(final String methodName, final WifiManager wifiManager) {
        final Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switcher(true);

        while (true) {
            if (isCorrectSimCard()) {
                onTick();
            }
            try {
                TimeUnit.SECONDS.sleep(15);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }

        }
    }

    private void onTick() {
        Calendar c = Calendar.getInstance();
        timeOn.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        timeOff.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // Scheduler part:
        if (isSchedulerOn()) {
            if (c.after(timeOff) && c.before(timeOn)) {
                onSchedulerInside();
            } else {
                on();
            }
        } else {
            on();
        }
    }

    private void on() {
        // Checks if Tethering is working
        if (isActivatedTethering() && !isSharingWiFi()) {
            Log.w(TAG, "Tethering turning on...");
            setWifiTetheringEnabled(true);
        }

        // Checks if 3G connection is established
        if (isActivated3G() && !isConnected(getApplicationContext())) {
            Log.w(TAG, "3G turning on...");
            setMobileDataEnabled(true);
        }
    }

    /**
     * Trigger when current time is inside schedule period
     */
    private void onSchedulerInside() {
        //    if (state) {
        switcher(false);
        Log.i(TAG, "Scheduler turned off connection and tethering");
        //    }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        onChangeProperties();
    }

    private void onChangeProperties() {
        DateFormat formatter = new SimpleDateFormat("HH:mm");

        timeOff = Calendar.getInstance();
        timeOn = Calendar.getInstance();
        try {
            timeOff.setTime(formatter.parse(prefs.getString(AppProperties.TIME_OFF, "")));
            timeOn.setTime(formatter.parse(prefs.getString(AppProperties.TIME_ON, "")));
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * @param state
     */
    private void switcher(boolean state) {
        if (isCorrectSimCard()) {
            Log.i(TAG, "Switch 3G and tethering to state=" + state);
            if (isActivated3G()) {
                setMobileDataEnabled(state);
            }
            if (isActivatedTethering()) {
                setWifiTetheringEnabled(state);
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
                    method.invoke(wifiManager, null, enable);
                } catch (Exception ex) {
                    Log.e(TAG, "Switch on tethering", ex);
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG);
                }
                break;
            }
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        Context context = getApplicationContext();
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
            Log.e(TAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.getMessage());
        }

        return false;
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(AppProperties.ACTIVATE_TETHERING, false);
    }

    private boolean isSchedulerOn() {
        return prefs.getBoolean(AppProperties.SCHEDULER, false);
    }

    private boolean isActivated3G() {
        return prefs.getBoolean(AppProperties.ACTIVATE_3G, false);
    }

    private boolean isActivateOnStartup() {
        return prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false);
    }

    private boolean isCorrectSimCard() {
        if (!prefs.getString(AppProperties.SIMCARD, "").isEmpty()) {
            TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String simCard = tMgr.getSimSerialNumber();
            return simCard != null && prefs.getString(AppProperties.SIMCARD, "").equals(simCard);
        } else {
            return true;
        }
    }

    /**
     * Check if there is any 3G connectivity
     *
     * @param context
     * @return
     */
    public boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }
}
