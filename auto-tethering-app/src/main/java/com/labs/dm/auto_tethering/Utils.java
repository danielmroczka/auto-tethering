package com.labs.dm.auto_tethering;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.widget.Toast;
import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.service.ServiceHelper;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by Daniel Mroczka
 */
public class Utils {

    private static final int MEAN_EARTH_RADIUS = 6371000;

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
        Map<Integer, String> map = new HashMap<>();
        map.put(6, "Mon");
        map.put(5, "Tue");
        map.put(4, "Wed");
        map.put(3, "Thu");
        map.put(2, "Fri");
        map.put(1, "Sat");
        map.put(0, "Sun");

        for (int i = binary.length() - 1; i >= 0; i--) {
            if ("1".equals(binary.substring(i, i + 1))) {
                result += map.get(i) + " ";
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
                MyLog.e("Util", e.getMessage());
            }
        }
    }

    public static int getWidgetId(Intent intent) {
        Bundle extras = intent.getExtras();
        int appWidgetId = INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
        }
        return appWidgetId;
    }

    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
        return false;
    }

    public static List<BluetoothDevice> getBluetoothDevices(Context context, SharedPreferences prefs) {
        ServiceHelper serviceHelper = new ServiceHelper(context);
        Set<BluetoothDevice> allBondedDevices = serviceHelper.getBondedDevices();
        List<BluetoothDevice> devicesToCheck = new ArrayList<>();
        List<String> preferredDevices = findPreferredDevices(prefs);
        for (String pref : preferredDevices) {
            for (BluetoothDevice device : allBondedDevices) {
                if (device.getName().equals(pref)) {
                    devicesToCheck.add(device);
                    break;
                }
            }
        }
        return devicesToCheck;
    }

    /**
     * Returns the list of preferred devices ordered by the last time connection
     *
     * @return
     */
    public static List<String> findPreferredDevices(final SharedPreferences prefs) {
        Map<String, ?> map = prefs.getAll();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().startsWith("bt.devices.")) {
                list.add(String.valueOf(entry.getValue()));
            }
        }
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String deviceName1, String deviceName2) {
                long lastConnectTime1 = prefs.getLong("bt.last.connect." + deviceName1, 0);
                long lastConnectTime2 = prefs.getLong("bt.last.connect." + deviceName2, 0);
                return (int) (lastConnectTime2 - lastConnectTime1);
            }
        });
        return list;
    }

    /**
     * Returns current system setting whether Data Roaming is enabled
     *
     * @param context
     * @return
     */
    public static Boolean isDataRoamingEnabled(final Context context) {
        if (Build.VERSION.SDK_INT < 17) {
            return Settings.System.getInt(context.getContentResolver(), Settings.Secure.DATA_ROAMING, 0) == 1;
        } else {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.DATA_ROAMING, 0) == 1;
        }
    }

    public static void showToast(final Context context, final String text) {
        Handler h = new Handler(context.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static Cellular getCellInfo(Context context) {
        final TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int mnc = 0, mcc = 0, lac = 0, cid = 0;
        String networkOperator = tel.getNetworkOperator();
        if (!TextUtils.isEmpty(networkOperator)) {
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
        }

        if (tel.getCellLocation() instanceof GsmCellLocation) {
            lac = ((GsmCellLocation) tel.getCellLocation()).getLac();
            cid = ((GsmCellLocation) tel.getCellLocation()).getCid();
        } else if (tel.getCellLocation() instanceof CdmaCellLocation) {
            lac = ((CdmaCellLocation) tel.getCellLocation()).getSystemId();
            cid = ((CdmaCellLocation) tel.getCellLocation()).getBaseStationId();
        }

        return new Cellular(mcc, mnc, lac, cid);
    }

    /**
     * @return distance in meters
     */
    public static double calculateDistance(double srcLat, double srcLong, double destLat, double destLong) {

        double latDistance = Math.toRadians(srcLat - destLat);
        double lngDistance = Math.toRadians(srcLong - destLong);

        double a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2))
                + (Math.cos(Math.toRadians(srcLat)))
                * (Math.cos(Math.toRadians(destLat))
                * (Math.sin(lngDistance / 2))
                * (Math.sin(lngDistance / 2)));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return MEAN_EARTH_RADIUS * c;
    }

    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static Location getLastKnownLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return location;
    }

}
