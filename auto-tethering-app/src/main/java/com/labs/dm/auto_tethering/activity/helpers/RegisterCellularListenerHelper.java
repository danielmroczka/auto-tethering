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
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.CellGroup;
import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.provider.GoogleGeoLocationProvider;
import com.labs.dm.auto_tethering.service.ServiceHelper;

import java.util.Collections;
import java.util.Comparator;
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
        new GetLastLocationTask().execute();
        activateGroupAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return addGroup(activateList, "A");
            }
        });
        deactivateGroupAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return addGroup(deactivateList, "D");
            }
        });
    }

    private boolean addGroup(final PreferenceCategory list, final String type) {
        if (list.getPreferenceCount() > AppProperties.MAX_CELL_GROUPS_COUNT) {
            Toast.makeText(activity, "Exceed the limit of group limit (" + AppProperties.MAX_CELL_GROUPS_COUNT + ")!", Toast.LENGTH_LONG).show();
            return false;
        }
        LayoutInflater li = LayoutInflater.from(activity);
        View promptsView = li.inflate(R.layout.cellgroup_prompt, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Provide group name");
        final EditText input = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                input.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                input.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }, 250);

        builder.setView(promptsView);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String groupName = input.getText().toString();
                CellGroup cellGroup = new CellGroup(groupName, type, CellGroup.STATUS.ENABLED.getValue());
                long res = db.addOrUpdateCellGroup(cellGroup);
                if (res > 0) {
                    cellGroup.setId((int) res);
                    final PreferenceScreen groupItem = activity.getPreferenceManager().createPreferenceScreen(activity);
                    groupItem.setTitle(cellGroup.getName());
                    setIcon(groupItem, cellGroup);
                    new Thread(new LoadCellGroupTask(list, type)).start();
                } else {
                    Toast.makeText(activity, "Please provide unique group name", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
        return true;
    }

    private void setIcon(PreferenceScreen groupItem, CellGroup cellGroup) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            groupItem.setIcon((cellGroup.getStatus() == CellGroup.STATUS.ENABLED.getValue() ? R.drawable.ic_checked : R.drawable.ic_unchecked));
        }
    }

    private boolean addCell(PreferenceGroup list, CellGroup group, PreferenceScreen remove) {
        new AddCellTask(list, group, remove).execute();
        return true;
    }

    private class GetLastLocationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            MyLog.i("GPS", "LocationTask start");
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper();
            }
            final LocationManager locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);

            final MyLocationListener gpsListener = new MyLocationListener("GPS-Provider");
            final MyLocationListener networkListener = new MyLocationListener("Network-Provider");
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, gpsListener, Looper.getMainLooper());
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, networkListener, Looper.getMainLooper());
            }

            final Handler myHandler = new Handler(Looper.getMainLooper());
            myHandler.postDelayed(new Runnable() {
                public void run() {
                    MyLog.i("GPS", "LocationTask stop");
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.removeUpdates(gpsListener);
                    }
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.removeUpdates(networkListener);
                    }
                    loadGroups();
                }
            }, AppProperties.GPS_UPDATE_TIMEOUT);

            return null;
        }
    }

    private void loadGroups() {
        new Thread(new LoadCellGroupTask(activateList, "A")).start();
        new Thread(new LoadCellGroupTask(deactivateList, "D")).start();
    }

    private class AddCellTask extends AsyncTask<Void, Void, Void> {

        private PreferenceGroup list;
        private CellGroup cellGroup;
        private PreferenceScreen remove;

        public AddCellTask(PreferenceGroup list, CellGroup cellGroup, PreferenceScreen remove) {
            this.list = list;
            this.cellGroup = cellGroup;
            this.remove = remove;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Cellular current = Utils.getCellInfo(activity);

            if (!current.isValid()) {
                Utils.showToast(activity, "Cannot retrieve Cellular network info.\nPlease check the network access and try again");
                return null;
            } else if (list.getPreferenceCount() >= MAX_CELLULAR_ITEMS + 2) {
                Utils.showToast(activity, "Exceeded the limit of max. " + MAX_CELLULAR_ITEMS + " configured networks!");
                return null;
            }

            loadLocationFromService(current);
            current.setCellGroup(cellGroup.getId());

            List<Cellular> otherTypesCellulars = db.readAllCellular("A".equals(cellGroup.getType()) ? "D" : "A");
            if (otherTypesCellulars.indexOf(current) >= 0) {
                Utils.showToast(activity, "Cellular network  (" + current.getCid() + "/" + current.getLac() + ") is already added to " + ("A".equals(cellGroup.getType()) ? "deactivation" : "activation") + " group list.\nCannot be added both to activation and deactivation lists!");
                return null;
            }

            long id = db.addOrUpdateCellular(current);

            if (id > 0) {
                current.setId((int) id);
                final CheckBoxPreference checkBox = createCheckBox(current, Utils.getBestLocation(activity));

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.addPreference(checkBox);
                        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
                        Utils.showToast(activity, "Cellular network has been added");
                    }
                });

                loadGroups();
            } else {
                Utils.showToast(activity, "Cellular network (" + current.toString() + ") is already added to group!");
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

    private class LoadCellGroupTask implements Runnable {
        private final PreferenceCategory list;
        private final String type;

        public LoadCellGroupTask(final PreferenceCategory list, final String type) {
            this.list = list;
            this.type = type;
        }

        @Override
        public void run() {
            for (int i = list.getPreferenceCount() - 1; i > 0; i--) {
                if (!list.getPreference(i).getKey().startsWith("cell.")) {
                    list.removePreference(list.getPreference(i));
                }
            }

            List<CellGroup> col = db.loadCellGroup(type);

            for (final CellGroup group : col) {
                loadCellGroup(list, group);
            }
        }
    }

    private void loadCellGroup(final PreferenceCategory list, final CellGroup group) {
        //final CellGroupPreference groupItem = new CellGroupPreference(list, group, activity);//activity.getPreferenceManager().createPreferenceScreen(activity);
        final PreferenceScreen groupItem = activity.getPreferenceManager().createPreferenceScreen(activity);
        groupItem.setTitle(group.getName());
        groupItem.setKey(String.valueOf(group.getId()));
        setIcon(groupItem, group);

        final PreferenceScreen toggle = activity.getPreferenceManager().createPreferenceScreen(activity);
        setIcon(toggle, group);
        setTitle(toggle, group.getStatus() == CellGroup.STATUS.ENABLED.getValue() ? "Group enabled" : "Group disabled");
        toggle.setSummary(group.getStatus() == CellGroup.STATUS.ENABLED.getValue() ? "Tap to disable group" : "Tap to enable group");
        toggle.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (db.toggleCellGroup(group) > 0) {
                    if (group.getStatus() == CellGroup.STATUS.ENABLED.getValue()) {
                        group.setStatus(CellGroup.STATUS.DISABLED.getValue());
                        toggle.setTitle("Group disabled");
                        toggle.setSummary("Tap to enable group");
                    } else {
                        group.setStatus(CellGroup.STATUS.ENABLED.getValue());
                        toggle.setTitle("Group enabled");
                        toggle.setSummary("Tap to disable group");
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        setIcon(toggle, group);
                        setIcon(groupItem, group);
                        loadGroups();
                    }
                }
                return true;
            }
        });

        PreferenceScreen removeGroup = activity.getPreferenceManager().createPreferenceScreen(activity);
        removeGroup.setTitle("Remove group " + group.getName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            removeGroup.setIcon(R.drawable.ic_trash);
        }
        removeGroup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.warning)
                        .setMessage("Do you want to remove current group?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (db.removeCellGroup(group.getId()) > 0) {
                                    loadGroups();
                                    groupItem.getDialog().dismiss();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null
                        ).show();
                return true;
            }
        });

        PreferenceScreen add = activity.getPreferenceManager().createPreferenceScreen(activity);
        add.setTitle("Add current cell");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            add.setIcon(R.drawable.ic_add);
        }

        final PreferenceScreen remove = activity.getPreferenceManager().createPreferenceScreen(activity);
        remove.setTitle("Remove selected cell");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            remove.setIcon(R.drawable.ic_remove);
        }

        PreferenceCategory groupCategory = new PreferenceCategory(activity);
        groupCategory.setTitle("Group operations");
        final PreferenceCategory cellCategory = new PreferenceCategory(activity);
        cellCategory.setTitle("Cellular network list");
        groupItem.addPreference(groupCategory);
        groupItem.addPreference(cellCategory);

        groupCategory.addPreference(toggle);
        groupCategory.addPreference(removeGroup);
        cellCategory.addPreference(add);
        cellCategory.addPreference(remove);

        add.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                addCell(cellCategory, group, remove);
                return true;
            }
        });

        remove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                removeCell(cellCategory, remove);
                remove.setEnabled(cellCategory.getPreferenceCount() > ITEM_COUNT);
                return true;
            }
        });

        final Location location = Utils.getBestLocation(activity);
        List<Cellular> cells = db.readCellular(group.getId());
        Collections.sort(cells, new Comparator<Cellular>() {
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
        for (Cellular cellular : cells) {
            if (!cellular.hasLocation()) {
                loadLocationFromService(cellular);
                db.addOrUpdateCellular(cellular);
            }
            CheckBoxPreference chk = createCheckBox(cellular, location);
            cellCategory.addPreference(chk);
        }

        groupItem.setSummary("Cells: " + cells.size());

        remove.setEnabled(cellCategory.getPreferenceCount() > ITEM_COUNT);
        list.addPreference(groupItem);
    }

    private void setTitle(PreferenceScreen toggle, String title) {
        toggle.setTitle(title);
    }

    private boolean removeCell(PreferenceGroup list, PreferenceScreen remove) {
        int changed = 0;

        for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
            Preference pref = list.getPreference(idx);
            if (pref instanceof CheckBoxPreference) {
                boolean status = ((CheckBoxPreference) pref).isChecked();
                if (status && !pref.getKey().startsWith("cell")) {
                    if (db.removeCellular(pref.getKey()) > 0) {
                        list.removePreference(pref);
                        changed++;
                    }
                }
            }
        }
        String text = changed == 0 ? "Please select any item" : (changed > 1) ? changed + " cellular networks have been removed" : "Cellular network has been removed";
        if (changed > 0) {
            loadGroups();
        }
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
        return true;
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

    private void loadLocationFromService(Cellular item) {
        if (new ServiceHelper(activity).isConnectedtToInternet()) {
            new GoogleGeoLocationProvider().loadLocationFromService(item);
        }
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
