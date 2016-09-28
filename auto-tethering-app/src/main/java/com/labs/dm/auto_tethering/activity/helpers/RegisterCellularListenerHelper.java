package com.labs.dm.auto_tethering.activity.helpers;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.widget.Toast;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.*;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Cellular;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.MAX_CELLULAR_ITEMS;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class RegisterCellularListenerHelper extends AbstractRegisterHelper {

    private final static int ITEM_COUNT = 3;

    public RegisterCellularListenerHelper(MainActivity activity) {
        super(activity);
        final TelephonyManager telManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        int events = PhoneStateListener.LISTEN_CELL_LOCATION;
        telManager.listen(new MyPhoneStateListener(), events);
    }

    public void registerUIListeners() {
        final PreferenceScreen activateAdd = getPreferenceScreen("cell.activate.add");
        final PreferenceScreen deactivateAdd = getPreferenceScreen("cell.deactivate.add");
        final PreferenceScreen activateRemove = getPreferenceScreen("cell.activate.remove");
        final PreferenceScreen deactivateRemove = getPreferenceScreen("cell.deactivate.remove");
        final PreferenceCategory activateList = getPreferenceCategory("cell.activate.list");
        final PreferenceCategory deactivateList = getPreferenceCategory("cell.deactivate.list");

    public RegisterCellularListenerHelper(MainActivity activity) {
        super(activity);
        final TelephonyManager telManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        int events = PhoneStateListener.LISTEN_CELL_LOCATION;
        telManager.listen(new MyPhoneStateListener(), events);
    }

    public void registerUIListeners() {
        new LocationTask().execute();

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
                    MyLog.d("Load JSON", result);
                    json = new JSONObject(result);
                    item.setLon(json.getDouble("lon"));
                    item.setLat(json.getDouble("lat"));
                    inputStream.close();
                }
            } catch (IOException e) {
                MyLog.e("HttpRequest", e.getMessage());
            } catch (JSONException e) {
                MyLog.e("JSONException", e.getMessage());
            }
        }
    }

    private class LocationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            MyLog.i("GPS", "LocationTask start");
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper();
            }
            final LocationManager locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);

            final MyLocationListener gpsListener = new MyLocationListener("GPS-Provider");
            final MyLocationListener networkListener = new MyLocationListener("Network-Provider");
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, gpsListener, Looper.getMainLooper());
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, networkListener, Looper.getMainLooper());

            final Handler myHandler = new Handler(Looper.getMainLooper());
            myHandler.postDelayed(new Runnable() {
                public void run() {
                    MyLog.i("GPS", "LocationTask stop");
                    locationManager.removeUpdates(gpsListener);
                    locationManager.removeUpdates(networkListener);
                    load(activateList, activateRemove, 'A');
                    load(deactivateList, deactivateRemove, 'D');
                }
            }, 7500);

            return null;
        }
    }

    private class AddTask extends AsyncTask<Void, Void, Void> {

        private PreferenceCategory list;
        private PreferenceScreen remove;
        private char type;

        public AddTask(PreferenceCategory list, PreferenceScreen remove, char type) {
            this.list = list;
            this.remove = remove;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Cellular current = Utils.getCellInfo(activity);

            if (!current.isValid()) {
                Utils.showToast(activity, "Cannot retrieve Cellular network info.\nPlease check the network access and try again");
                return null;
            } else if (list.getPreferenceCount() > ITEM_COUNT + MAX_CELLULAR_ITEMS) {
                Utils.showToast(activity, "Exceeded the limit of max. " + MAX_CELLULAR_ITEMS + " configured networks!");
                return null;
            }

            List<Cellular> activeList = db.readCellular('A');
            for (Cellular c : activeList) {
                if (current.theSame(c)) {
                    Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already on the activation list!");
                    return null;
                }
            }

            List<Cellular> deactiveList = db.readCellular('D');
            for (Cellular c : deactiveList) {
                if (current.theSame(c)) {
                    Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already on the deactivation list!");
                    return null;
                }
            }

            loadLocationFromService(current);
            current.setType(type);
            current.setName("");
            long id = db.addOrUpdateCellular(current);

            if (id > 0) {
                final CheckBoxPreference checkBox = createCheckBox(current, Utils.getBestLocation(activity), id);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.addPreference(checkBox);
                        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
                        Toast.makeText(activity, "Cellular network has been added", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return null;
        }

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(activity, R.style.MyTheme);
            progress.setCancelable(false);
            progress.setIndeterminateDrawable(activity.getResources().getDrawable(R.drawable.progress));
            progress.show();
        }

        @Override
        protected void onPostExecute(Void aLong) {
            progress.dismiss();
        }
    }

    private CheckBoxPreference createCheckBox(Cellular current, Location location, long id) {
        final CheckBoxPreference checkBox = new CheckBoxPreference(activity);
        String styledText = String.format("<small>CID: </small>%s <small>LAC: </small>%s", current.getCid(), current.getLac());
        checkBox.setTitle(Html.fromHtml(styledText));
        checkBox.setKey(String.valueOf(id));
        checkBox.setPersistent(false);

        if (current.hasLocation()) {
            Location location = Utils.getBestLocation(activity);
            double distance = Utils.calculateDistance(location, current);
            checkBox.setSummary(Utils.formatDistance(location, distance));
        } else {
            checkBox.setSummary("Distance: n/a");
        }
        return checkBox;
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
            List<Cellular> col = db.readCellular(type);

            for (Cellular item : col) {
                if (!item.hasLocation()) {
                    loadLocationFromService(item);
                    if (item.hasLocation()) {
                        db.addOrUpdateCellular(item);
                    }
                }
            }

            final Location location = Utils.getBestLocation(activity);

            if (location != null) {
                Collections.sort(col, new Comparator<Cellular>() {
                    @Override
                    public int compare(Cellular lhs, Cellular rhs) {
                        if (lhs.isValid() && rhs.isValid()) {
                            double distance1 = Utils.calculateDistance(location, lhs);
                            double distance2 = Utils.calculateDistance(location, rhs);
                            return (int) (distance1 - distance2);
                        } else {
                            return -1;
                        }
                    }
                });
            }
            for (Cellular item : col) {
                final CheckBoxPreference checkBox = createCheckBox(item, location, item.getId());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.addPreference(checkBox);
                    }
                });
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
                }
            });
        }
    }

    private boolean remove(PreferenceCategory list, PreferenceScreen remove) {
        boolean changed = false;

        for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
            Preference pref = list.getPreference(idx);
            if (pref instanceof CheckBoxPreference) {
                boolean status = ((CheckBoxPreference) pref).isChecked();
                if (status && !pref.getKey().startsWith("cell")) {
                    if (db.removeCellular(pref.getKey()) > 0) {
                        list.removePreference(pref);
                        changed = true;
                    }
                }
            }
        }

        String text = changed ? "Cellular network has been removed" : "Please select any item";
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
        return true;
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);
            final PreferenceScreen cell = getPreferenceScreen("cell.current");
            Cellular current = Utils.getCellInfo(activity);
            String styledText = String.format("<small>CID:</small><font color='#00FF40'>%s</font> <small>LAC:</small><font color='#00FF40'>%s</font>", current.getCid(), current.getLac());
            cell.setTitle("Current Cellular Network:");
            cell.setSummary(Html.fromHtml(styledText));
        }
    }

    private class MyLocationListener implements LocationListener {
        private String TAG;

        public MyLocationListener(String TAG) {
            this.TAG = TAG;
        }

        @Override
        public void onLocationChanged(Location location) {
            MyLog.i(TAG, "onLocationChanged;accuracy=" + location.getAccuracy());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            MyLog.i(TAG, "onStatusChanged;status=" + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

}
