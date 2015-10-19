package com.labs.dm.auto_tethering;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Daniel Mroczka
 */
public class Utils {

    public static boolean validateTime(final String time) {
        String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
        Pattern pattern = Pattern.compile(TIME24HOURS_PATTERN);
        Matcher matcher = pattern.matcher(time);
        return matcher.matches();
    }

    public static int connectedClients() {
        int res = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("IP")) {
                    res++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    public static boolean exists(String commaSeparatedString, String item) {
        String[] sets = commaSeparatedString.split(",");
        for (String set : sets) {
            if (item.equals(set)) {
                return true;
            }
        }

        return false;
    }

    public static String add(String commaSeparatedString, String item) {
        if (!exists(commaSeparatedString, item)) {
            if (!commaSeparatedString.isEmpty()) {
                commaSeparatedString += ",";
            }
            commaSeparatedString += item;
        }

        return commaSeparatedString;
    }

    public static String remove(String commaSeparatedString, String item) {
        StringBuilder sb = new StringBuilder();
        for (String s : commaSeparatedString.split(",")) {
            if (!item.equalsIgnoreCase(s)) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static void setMobileDataEnabled(Context context, boolean enabled) {
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
            Log.e("", "Switch on 3G", e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    public static void setWifiTetheringEnabled(Context context, WifiManager wifiManager, boolean enable) {
        wifiManager.setWifiEnabled(false);
        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    Log.i("", "setWifiTetheringEnabled to " + enable);
                    method.invoke(wifiManager, null, enable);
                } catch (Exception ex) {
                    Log.e("", "Switch on tethering", ex);
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
}
