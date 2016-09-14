package com.labs.dm.auto_tethering.activity.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.CellInfo;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class RegisterCellularListenerHelper {

    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final static int ITEM_COUNT = 3;

    public RegisterCellularListenerHelper(MainActivity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
    }

    private String[] getCidsActivate() {
        return prefs.getString("cell.activate.cids", "").split(",");
    }

    private String[] getCidsDeactivate() {
        return prefs.getString("cell.deactivate.cids", "").split(",");
    }

    public void registerCellularNetworkListener() {
        final PreferenceScreen activateAdd = (PreferenceScreen) activity.findPreference("cell.activate.add");
        final PreferenceScreen deactivateAdd = (PreferenceScreen) activity.findPreference("cell.deactivate.add");
        final PreferenceScreen activateRemove = (PreferenceScreen) activity.findPreference("cell.activate.remove");
        final PreferenceScreen deactivateRemove = (PreferenceScreen) activity.findPreference("cell.deactivate.remove");
        final PreferenceCategory activateList = (PreferenceCategory) activity.findPreference("cell.activate.list");
        final PreferenceCategory deactivateList = (PreferenceCategory) activity.findPreference("cell.deactivate.list");

        activateAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return add(activateList, activateRemove, "cell.activate.cids");
            }
        });

        deactivateAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return add(deactivateList, deactivateRemove, "cell.deactivate.cids");
            }
        });

        activateRemove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return remove(activateList, activateRemove, "cell.activate.cids");
            }
        });

        deactivateRemove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return remove(deactivateList, deactivateRemove, "cell.deactivate.cids");
            }
        });

        list(getCidsActivate(), activateList);
        list(getCidsDeactivate(), deactivateList);

        activateRemove.setEnabled(activateList.getPreferenceCount() > ITEM_COUNT);
        deactivateRemove.setEnabled(deactivateList.getPreferenceCount() > ITEM_COUNT);
    }

    private void list(String[] items, PreferenceCategory list) {
        for (String item : items) {
            if (!item.isEmpty()) {
                Thread th = new Thread(new Run(list, item));
                th.start();
            }
        }
    }

    private class Run implements Runnable {

        private String item;
        private PreferenceCategory list;

        public Run(PreferenceCategory list, String item) {
            this.item = item;
            this.list = list;
        }

        @Override
        public void run() {
            CellInfo cellInfo = new CellInfo(item);
            TelephonyManager manager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
            String networkOperator = manager.getNetworkOperator();
            int mcc = 0;
            int mnc = 0;
            if (TextUtils.isEmpty(networkOperator) == false) {
                mcc = Integer.parseInt(networkOperator.substring(0, 3));
                mnc = Integer.parseInt(networkOperator.substring(3));
            }

            String url = String.format("http://opencellid.org/cell/get?key=%s&mcc=%d&mnc=%d&lac=%d&cellid=%d&format=json", BuildConfig.OPENCELLID_KEY, mcc, mnc, cellInfo.getLac(), cellInfo.getCid());

            HttpClient httpclient = new DefaultHttpClient();
            double lon = 0, lat = 0;
            HttpGet httpget = new HttpGet(url);
            HttpResponse response;
            try {
                response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    String result = convertStreamToString(instream);
                    JSONObject json = new JSONObject(result);
                    lon = json.getDouble("lon");
                    lat = json.getDouble("lat");
                    instream.close();
                }
            } catch (Exception e) {
                Log.e("HttpRequest", e.getMessage());
            }

            CheckBoxPreference checkbox = new CheckBoxPreference(activity);
            checkbox.setTitle(item);

            LocationManager locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(false);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String provider = locationManager.getBestProvider(criteria, true);

            Location location = locationManager.getLastKnownLocation(provider);
            if (lat > 0 && lon > 0) {
                double distance = Utils.calculateDistance(location.getLatitude(), location.getLongitude(), lat, lon);
                checkbox.setSummary(String.format("Distance: %.0f m ± %.0f m", distance, location.getAccuracy()));
            } else {
                checkbox.setSummary("Distance: n/a");
            }

            list.addPreference(checkbox);
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
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

    private boolean add(PreferenceCategory list, PreferenceScreen remove, String key) {
        CellInfo cellInfo = new CellInfo(Utils.getCid(activity), Utils.getLac(activity));

        if (!cellInfo.isValid()) {
            return false;
        }

        for (String c : getCidsActivate()) {
            if (!c.isEmpty() && c.equals(cellInfo.toString())) {
                Toast.makeText(activity, "Cellular network (" + cellInfo.toString() + ") is already on the activation list", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        for (String c : getCidsDeactivate()) {
            if (!c.isEmpty() && c.equals(cellInfo.toString())) {
                Toast.makeText(activity, "Cellular network (" + cellInfo.toString() + ") is already on the deactivation list", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        Preference ps = new CheckBoxPreference(activity);
        ps.setTitle(cellInfo.toString());
        list.addPreference(ps);
        String location = prefs.getString(key, "");
        location = location + cellInfo.toString() + ",";
        prefs.edit().putString(key, location + ",").apply();
        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
        return false;
    }

    private boolean remove(PreferenceCategory list, PreferenceScreen remove, String key) {
        boolean changed = false;

        for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
            Preference pref = list.getPreference(idx);
            if (pref instanceof CheckBoxPreference) {
                boolean status = ((CheckBoxPreference) pref).isChecked();
                if (status && pref.getKey() == null) {
                    String cids = prefs.getString(key, "");
                    cids = cids.replace(pref.getTitle() + ",", "");
                    prefs.edit().putString(key, cids).apply();
                    list.removePreference(pref);
                    changed = true;
                }
            }
        }

        if (!changed) {
            Toast.makeText(activity, "Please select any item", Toast.LENGTH_LONG).show();
        }
        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
        return true;
    }
}
