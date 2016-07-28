package com.labs.dm.auto_tethering;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Daniel Mroczka
 */
public class Utils {

    public static boolean validateTime(final String time) {
        final String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
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

    public static String maskToDays(int mask) {
        String binary = String.format("%7s", Integer.toBinaryString(mask)).replace(' ', '0');
        String result = "";
        for (int i = binary.length() - 1; i >= 0; i--) {
            if ("1".equals(binary.substring(i, i + 1))) {
                switch (i) {
                    case 6:
                        result += "Mon ";
                        break;
                    case 5:
                        result += "Tue ";
                        break;
                    case 4:
                        result += "Wed ";
                        break;
                    case 3:
                        result += "Thu ";
                        break;
                    case 2:
                        result += "Fri ";
                        break;
                    case 1:
                        result += "Sat ";
                        break;
                    case 0:
                        result += "Sun ";
                        break;
                }
            }
        }
        result = result.trim().replaceAll(" ", ", ");
        return result;
    }

    public static int adapterDayOfWeek(int day) {
        SparseIntArray map = new SparseIntArray();
        map.put(1, 6);
        map.put(2, 0);
        map.put(3, 1);
        map.put(4, 2);
        map.put(5, 3);
        map.put(6, 4);
        map.put(7, 5);
        return map.get(day);
    }

    public static void broadcast(Context context, Intent intent) {
        if (intent != null && context != null) {
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                onPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e("Util", e.getMessage());
            }
        }
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
}
