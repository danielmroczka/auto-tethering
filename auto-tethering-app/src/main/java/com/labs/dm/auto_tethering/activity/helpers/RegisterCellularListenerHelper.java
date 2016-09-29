package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
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
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.CellGroup;
import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.ui.CellGroupPreference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.MAX_CELLULAR_ITEMS;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class RegisterCellularListenerHelper extends AbstractRegisterHelper {

    private final static int ITEM_COUNT = 2;
    private PreferenceScreen activateGroupAdd = getPreferenceScreen("cell.activate.group.add");
    private PreferenceScreen deactivateGroupAdd = getPreferenceScreen("cell.deactivate.group.add");
    private PreferenceCategory activateList = getPreferenceCategory("cell.activate.list");
    private PreferenceCategory deactivateList = getPreferenceCategory("cell.deactivate.list");

    public RegisterCellularListenerHelper(MainActivity activity) {
        super(activity);
        final TelephonyManager telManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        int events = PhoneStateListener.LISTEN_CELL_LOCATION;
        telManager.listen(new MyPhoneStateListener(), events);
    }

    public void registerUIListeners() {
        new LocationTask().execute();

        activateGroupAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return addGroup(activateList, 'A');
            }
        });
        deactivateGroupAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return addGroup(deactivateList, 'D');
            }
        });
    }

    private boolean addGroup(final PreferenceCategory list, final char type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Provide group name");
        final EditText input = new EditText(activity.getApplicationContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setPadding(1, 1, 1, 1);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String groupName = input.getText().toString();
                CellGroup cellGroup = new CellGroup(groupName, String.valueOf(type), CellGroup.STATUS.ENABLED.getValue());
                long res = db.addOrUpdateCellGroup(cellGroup);
                if (res > 0) {
                    cellGroup.setId((int) res);
                    CellGroupPreference group = new CellGroupPreference(list, cellGroup, activity);
                    list.addPreference(group);
                } else {
                    Toast.makeText(activity, "Please provide unique group name", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
        return true;
    }

    private boolean add(PreferenceScreen list, CellGroup group) {
        new AddTask(list, group).execute();
        return true;
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
                    loadGroups();
                }
            }, 5000);

            return null;
        }
    }

    private void loadGroups() {
        new Thread(new LoadTask(activateList, 'A')).start();
        new Thread(new LoadTask(deactivateList, 'D')).start();
    }

    private class AddTask extends AsyncTask<Void, Void, Void> {

        private PreferenceScreen list;
        private CellGroup cellGroup;

        public AddTask(PreferenceScreen list, CellGroup cellGroup) {
            this.list = list;
            this.cellGroup = cellGroup;
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

//            List<Cellular> activeList = db.readCellular('A');
//            for (Cellular c : activeList) {
//                if (current.theSame(c)) {
//                    Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already on the activation list!");
//                    return null;
//                }
//            }
//
//            List<Cellular> deactiveList = db.readCellular('D');
//            for (Cellular c : deactiveList) {
//                if (current.theSame(c)) {
//                    Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already on the deactivation list!");
//                    return null;
//                }
//            }

            loadLocationFromService(current);
            current.setName("");
            current.setCellGroup(cellGroup.getId());
            long id = db.addOrUpdateCellular(current);

            if (id > 0) {
                final CheckBoxPreference checkBox = createCheckBox(current, Utils.getBestLocation(activity));

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.addPreference(checkBox);
                        //remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
                        Toast.makeText(activity, "Cellular network has been added", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already added into group!");
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

    private CheckBoxPreference createCheckBox(Cellular current, Location location) {
        final CheckBoxPreference checkBox = new CheckBoxPreference(activity);
        String styledText = String.format("<small>CID: </small>%s <small>LAC: </small>%s", current.getCid(), current.getLac());
        checkBox.setTitle(Html.fromHtml(styledText));
        checkBox.setKey(String.valueOf(current.getId()));
        checkBox.setPersistent(false);

        if (current.hasLocation()) {
            double distance = Utils.calculateDistance(location, current);
            checkBox.setSummary(Utils.formatDistance(location, distance));
        } else {
            checkBox.setSummary("Distance: n/a");
        }
        return checkBox;
    }

    private class LoadTask implements Runnable {
        private final PreferenceCategory list;
        private final char type;

        public LoadTask(final PreferenceCategory list, final char type) {
            this.list = list;
            this.type = type;
        }

        @Override
        public void run() {
            List<CellGroup> col = db.loadCellGroup(String.valueOf(type));

            for (final CellGroup group : col) {
                //CellGroupPreference cell = new CellGroupPreference(list, group, activity);
                //list.addPreference(cell);

                final PreferenceScreen groupItem = activity.getPreferenceManager().createPreferenceScreen(activity);
                groupItem.setTitle(group.getName());

                PreferenceScreen child = activity.getPreferenceManager().createPreferenceScreen(activity);


                PreferenceScreen add = activity.getPreferenceManager().createPreferenceScreen(activity);
                add.setTitle("Add cell");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    add.setIcon(R.drawable.ic_add);
                }

                final PreferenceScreen remove = activity.getPreferenceManager().createPreferenceScreen(activity);
                remove.setTitle("Remove cell");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    remove.setIcon(R.drawable.ic_remove);
                }

                groupItem.addPreference(add);

                add.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        add(groupItem, group);
                        remove.setEnabled(groupItem.getPreferenceCount() > ITEM_COUNT);
                        return true;
                    }
                });

                remove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        remove(groupItem, remove);
                        remove.setEnabled(groupItem.getPreferenceCount() > ITEM_COUNT);
                        return true;
                    }
                });

                groupItem.addPreference(remove);

                for (Cellular cellular : db.readCellular(group.getId())) {
                    if (!cellular.hasLocation()) {
                        loadLocationFromService(cellular);
                        db.addOrUpdateCellular(cellular);
                    }
                    CheckBoxPreference chk = createCheckBox(cellular, Utils.getBestLocation(activity));
                    groupItem.addPreference(chk);
                }

                remove.setEnabled(groupItem.getPreferenceCount() > ITEM_COUNT);


                list.addPreference(groupItem);
            }
        }
    }

    private boolean remove(PreferenceScreen list, PreferenceScreen remove) {
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
