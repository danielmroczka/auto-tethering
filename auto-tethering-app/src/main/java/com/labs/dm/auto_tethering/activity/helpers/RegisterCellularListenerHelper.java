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
                Loc loc = new Loc(Utils.getCid(activity), Utils.getLac(activity));

                if (!loc.isValid()) {
                    return false;
                }

                for (String c : getCidsActivate()) {
                    if (!c.isEmpty() && c.equals(loc.getLoc())) {
                        Toast.makeText(activity, "Current Cellular Network is already on the activation list", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
                for (String c : getCidsDeactivate()) {
                    if (!c.isEmpty() && c.equals(loc.getLoc())) {
                        Toast.makeText(activity, "Current Cellular Network is already on the deactivation list", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }

                Preference ps = new CheckBoxPreference(activity);
                ps.setTitle(loc.getLoc());
                activateList.addPreference(ps);
                prefs.edit().putString("cell.activate.cids", loc.getLoc() + ",").apply();

                activateRemove.setEnabled(activateList.getPreferenceCount() > 3);
                return false;
            }
        });

        deactivateAdd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Loc loc = new Loc(Utils.getCid(activity), Utils.getLac(activity));

                if (!loc.isValid()) {
                    return false;
                }

                for (String c : getCidsActivate()) {
                    if (!c.isEmpty() && c.equals(loc.getLoc())) {
                        Toast.makeText(activity, "Current Cellular Network is already on the activation list", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
                for (String c : getCidsDeactivate()) {
                    if (!c.isEmpty() && c.equals(loc.getLoc())) {
                        Toast.makeText(activity, "Current Cellular Network is already on the deactivation list", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }

                Preference ps = new CheckBoxPreference(activity);
                ps.setTitle(loc.getLoc());
                deactivateList.addPreference(ps);
                prefs.edit().putString("cell.deactivate.cids", loc.getLoc() + ",").apply();
                deactivateRemove.setEnabled(deactivateList.getPreferenceCount() > 3);
                return false;
            }
        });

        activateRemove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean changed = false;

                for (int idx = activateList.getPreferenceCount() - 1; idx >= 0; idx--) {
                    Preference pref = activateList.getPreference(idx);
                    if (pref instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) pref).isChecked();
                        if (status) {
                            String cids = prefs.getString("cell.activate.cids", "");
                            cids = cids.replace(pref.getTitle() + ",", "");
                            prefs.edit().putString("cell.activate.cids", cids).apply();
                            activateList.removePreference(pref);
                            changed = true;
                        }
                    }
                }

                if (!changed) {
                    Toast.makeText(activity, "Please select any item", Toast.LENGTH_LONG).show();
                }
                activateRemove.setEnabled(activateList.getPreferenceCount() > 3);
                return true;
            }
        });

        deactivateRemove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean changed = false;

                for (int idx = deactivateList.getPreferenceCount() - 1; idx >= 0; idx--) {
                    Preference pref = deactivateList.getPreference(idx);
                    if (pref instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) pref).isChecked();
                        if (status) {
                            String cids = prefs.getString("cell.deactivate.cids", "");
                            cids = cids.replace(pref.getTitle() + ",", "");
                            prefs.edit().putString("cell.deactivate.cids", cids).apply();
                            deactivateList.removePreference(pref);
                            changed = true;
                        }
                    }
                }

                if (!changed) {
                    Toast.makeText(activity, "Please select any item", Toast.LENGTH_LONG).show();
                }
                deactivateRemove.setEnabled(deactivateList.getPreferenceCount() > 3);
                return true;
            }
        });


        for (String item : getCidsActivate()) {
            if (!item.isEmpty()) {
                CheckBoxPreference checkbox = new CheckBoxPreference(activity);
                checkbox.setTitle(item);
                activateList.addPreference(checkbox);
            }
        }

        for (String item : getCidsDeactivate()) {
            if (!item.isEmpty()) {
                CheckBoxPreference checkbox = new CheckBoxPreference(activity);
                checkbox.setTitle(item);
                deactivateList.addPreference(checkbox);
            }
        }

        activateRemove.setEnabled(activateList.getPreferenceCount() > 3);
        deactivateRemove.setEnabled(deactivateList.getPreferenceCount() > 3);
    }
}
