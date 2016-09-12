package com.labs.dm.auto_tethering.activity.helpers;

import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import com.labs.dm.auto_tethering.Loc;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;

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
                CheckBoxPreference checkbox = new CheckBoxPreference(activity);
                checkbox.setTitle(item);
                list.addPreference(checkbox);
            }
        }
    }

    private boolean add(PreferenceCategory list, PreferenceScreen remove, String key) {
        Loc loc = new Loc(Utils.getCid(activity), Utils.getLac(activity));

        if (!loc.isValid()) {
            return false;
        }

        for (String c : getCidsActivate()) {
            if (!c.isEmpty() && c.equals(loc.getLoc())) {
                Toast.makeText(activity, "Current cellular network (" + loc.getLoc() + ") is already on the activation list", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        for (String c : getCidsDeactivate()) {
            if (!c.isEmpty() && c.equals(loc.getLoc())) {
                Toast.makeText(activity, "Current cellular network (" + loc.getLoc() + ") is already on the deactivation list", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        Preference ps = new CheckBoxPreference(activity);
        ps.setTitle(loc.getLoc());
        list.addPreference(ps);
        prefs.edit().putString(key, loc.getLoc() + ",").apply();
        remove.setEnabled(list.getPreferenceCount() > ITEM_COUNT);
        return false;

    }

    private boolean remove(PreferenceCategory list, PreferenceScreen remove, String key) {
        boolean changed = false;

        for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
            Preference pref = list.getPreference(idx);
            if (pref instanceof CheckBoxPreference) {
                boolean status = ((CheckBoxPreference) pref).isChecked();
                if (status) {
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
