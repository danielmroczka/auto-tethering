package com.labs.dm.auto_tethering.service;

import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static android.os.BatteryManager.BATTERY_PLUGGED_USB;

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
    public boolean isTetheringWiFi() {
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
     * Returns true if device is connected to usb or charger
     *
     * @return
     */
    public boolean isPluggedToPower() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int chargePlug = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) : 0;
        return chargePlug == BATTERY_PLUGGED_USB || chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    }

    public float batteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 0;
        return level / (float) scale;
    }

    /**
     * Returns declared portable Wi-Fi hotspot network SSID.
     *
     * @return network SSID
     */
    public String getTetheringSSID() {
        WifiConfiguration cfg = getWifiApConfiguration(context);
        return cfg != null ? cfg.SSID : "";
    }

    /**
     * Returns true if internet connection provided by mobile is currently active.
     *
     * @return
     */
    public boolean isConnectedToInternet() {
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
     * Works only for Android < 5.0
     *
     * @param enabled
     */
    public void setMobileDataEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "Unimplemented setMobileDataEnabled on Android 5.0!");
            return;
        }
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
        final Method method = getWifiManagerMethod("getWifiApConfiguration", wifiManager);
        if (method != null) {
            try {
                return (WifiConfiguration) method.invoke(wifiManager);
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

    @Deprecated
    public void usbTethering(boolean value) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.d(TAG, "test enable usb tethering");
        String[] available = null;
        int code = -1;
        Method[] wmMethods = cm.getClass().getDeclaredMethods();

        for (Method method : wmMethods) {
            if (method.getName().equals("getTetherableIfaces")) {

                try {
                    available = (String[]) method.invoke(cm);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    return;
                }
                break;
            }
        }

        for (Method method : wmMethods) {
            if (method.getName().equals("tether")) {
                try {
                    code = (Integer) method.invoke(cm, available != null ? available[0] : null);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    return;
                }
                break;
            }
        }

        if (code == 0)
            Log.d(TAG, "Enable usb tethering successfully!");
        else
            Log.d(TAG, "Enable usb tethering failed!");
    }

    public static long getDataUsage() {
        return TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
    }

    /**
     * Checks if service is running
     *
     * @param serviceClass
     * @return
     */
    public boolean isServiceRunning(Class<? extends Service> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isBluetoothActive() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public void setBluetoothStatus(boolean bluetoothStatus) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothStatus && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        } else if (!bluetoothStatus && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    public void setBlockingBluetoothStatus(boolean bluetoothStatus) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        setBluetoothStatus(bluetoothStatus);
        long time = SystemClock.currentThreadTimeMillis();
        while (adapter.isEnabled() != bluetoothStatus && SystemClock.currentThreadTimeMillis() - time < 3000) {
        }
    }

    /**
     * Retrieving bonded devices requires switched on Bluetooth connection.
     * In case if BT connection is not active it will turn on read all bonded devices and then restore to initial state.
     *
     * @return
     */
    public Set<BluetoothDevice> getBondedDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean state = adapter.isEnabled();
        if (!state) {
            adapter.enable();
            long time = SystemClock.currentThreadTimeMillis();
            while (!adapter.isEnabled() && SystemClock.currentThreadTimeMillis() - time < 3000) {

            }
        }
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (!state) {
            adapter.disable();
        }
        return pairedDevices;
    }
}
