package com.labs.dm.auto_tethering.activity.helpers;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.db.DBManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.List;

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
                return add(activateList, activateRemove, 'A');
            }
        });

        deactivateAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return add(deactivateList, deactivateRemove, 'D');
            }
        });

        activateRemove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return remove(activateList, activateRemove);
            }
        });

        deactivateRemove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return remove(deactivateList, deactivateRemove);
            }
        });

        load(activateList, activateRemove, 'A');
        load(deactivateList, deactivateRemove, 'D');
    }

    private boolean add(PreferenceCategory list, PreferenceScreen remove, char type) {
        new AddTask(list, remove, type).execute();
        return true;
    }

    private void load(PreferenceCategory list, PreferenceScreen remove, char type) {
        Thread th = new Thread(new LoadTask(list, remove, type));
        th.start();
    }

    private void loadLocationFromService(Cellular item) {
        JSONObject json;
        String url = String.format("http://opencellid.org/cell/get?key=%s&mcc=%d&mnc=%d&lac=%d&cellid=%d&format=json", BuildConfig.OPENCELLID_KEY, item.getMcc(), item.getMnc(), item.getLac(), item.getCid());

        if (!item.hasLocation()) {
            HttpClient httpclient = new DefaultHttpClient();

            HttpGet httpget = new HttpGet(url);
            HttpResponse response;
            try {
                response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = entity.getContent();
                    String result = Utils.convertStreamToString(inputStream);
                    Log.d("Load JSON", result);
                    json = new JSONObject(result);
                    item.setLon(json.getDouble("lon"));
                    item.setLat(json.getDouble("lat"));
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e("HttpRequest", e.getMessage());
            }
        }
    }

    private class AddTask extends AsyncTask<Void, Void, Long> {

        private PreferenceCategory list;
        private PreferenceScreen remove;
        private char type;

        public AddTask(PreferenceCategory list, PreferenceScreen remove, char type) {
            this.list = list;
            this.remove = remove;
            this.type = type;
        }

        @Override
        protected Long doInBackground(Void... params) {

            Cellular current = Utils.getCellInfo(activity);

            List<Cellular> activeList = DBManager.getInstance(activity).readCellular('A');

            for (Cellular c : activeList) {
                if (current.theSame(c)) {
                    Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already on the activation list!");
                    return 0L;
                }
            }

            List<Cellular> deactiveList = DBManager.getInstance(activity).readCellular('D');
            for (Cellular c : deactiveList) {
                if (current.theSame(c)) {
                    Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already on the deactivation list!");
                    return 0L;
                }
            }

            loadLocationFromService(current);
            current.setType(type);
            current.setName("");
            long id = DBManager.getInstance(activity).addCellular(current);

            if (id > 0) {
                final CheckBoxPreference checkBox = new CheckBoxPreference(activity);
                checkBox.setTitle(current.toString());
                checkBox.setKey(String.valueOf(id));
                checkBox.setChecked(false);
                if (current.hasLocation()) {
                    Location location = Utils.getLastKnownLocation(activity);
                    double distance = Utils.calculateDistance(location.getLatitude(), location.getLongitude(), current.getLat(), current.getLon());
                    checkBox.setSummary(String.format("Distance: %.0f m ± %.0f m", distance, location.getAccuracy()));
                } else {
                    checkBox.setSummary("Distance: n/a");
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.addPreference(checkBox);
                        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
                    }
                });
                return id;
            }
            return null;
        }

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(activity);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected void onPostExecute(Long aLong) {
            progress.dismiss();
        }
    }

    private class LoadTask implements Runnable {
        private final PreferenceCategory list;
        private final PreferenceScreen remove;
        private final char type;

        public LoadTask(final PreferenceCategory list, final PreferenceScreen remove, final char type) {
            this.list = list;
            this.remove = remove;
            this.type = type;
        }

        @Override
        public void run() {
            List<Cellular> col = DBManager.getInstance(activity).readCellular(type);

            for (Cellular item : col) {
                loadLocationFromService(item);
                CheckBoxPreference checkbox = new CheckBoxPreference(activity);
                checkbox.setKey(String.valueOf(item.getId()));
                checkbox.setTitle(item.toString());
                checkbox.setChecked(false);

                if (item.hasLocation()) {
                    Location location = Utils.getLastKnownLocation(activity);
                    double distance = Utils.calculateDistance(location.getLatitude(), location.getLongitude(), item.getLat(), item.getLon());
                    checkbox.setSummary(String.format("Distance: %.0f m ± %.0f m", distance, location.getAccuracy()));
                } else {
                    checkbox.setSummary("Distance: n/a");
                }

                list.addPreference(checkbox);

            }

            remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
        }
    }

    private boolean remove(PreferenceCategory list, PreferenceScreen remove) {
        boolean changed = false;

        for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
            Preference pref = list.getPreference(idx);
            if (pref instanceof CheckBoxPreference) {
                boolean status = ((CheckBoxPreference) pref).isChecked();
                if (status && !pref.getKey().startsWith("cell")) {
                    if (DBManager.getInstance(activity).removeCellular(pref.getKey()) > 0) {
                        list.removePreference(pref);
                        changed = true;
                    }
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
