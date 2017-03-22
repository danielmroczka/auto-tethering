package com.labs.dm.auto_tethering.activity.helpers;

import android.content.DialogInterface;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.WiFiTethering;
import com.labs.dm.auto_tethering.ui.dialog.WiFiTetheringDialog;

import java.util.List;

/**
 * Created by Daniel Mroczka on 21-Mar-17.
 */

public class RegisterWiFiListListenerHelper extends AbstractRegisterHelper {

    private final int CONST_ITEMS = 3; // Skip constant elements on list ADD, MODIFY, REMOVE

    public RegisterWiFiListListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        final PreferenceCategory list = getPreferenceCategory("wifi.list");
        final PreferenceScreen remove = getPreferenceScreen("wifi.remove.device");
        final PreferenceScreen modify = getPreferenceScreen("wifi.modify.device");

        List<WiFiTethering> nets = db.readWiFiTethering();
        clean("wifi.list");
        for (WiFiTethering net : nets) {
            CheckBoxPreference item = new CheckBoxPreference(activity);
            item.setKey(String.valueOf(net.getId()));
            item.setTitle("SSID: " + net.getSsid());
            item.setSummary("Security: " + net.getType().name() + " Channel: " + net.getChannel());
            item.setPersistent(false);
            list.addPreference(item);
        }

        remove.setEnabled(list.getPreferenceCount() > CONST_ITEMS);
        modify.setEnabled(list.getPreferenceCount() > CONST_ITEMS);

        getPreferenceScreen("wifi.add.device").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final WiFiTetheringDialog dialog = new WiFiTetheringDialog(activity, null);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dlg) {
                        WiFiTethering entity = dialog.getEntity();
                        if (entity != null) {
                            long id = db.addOrUpdateWiFiTethering(entity);

                            if (id > 0) {
                                Preference item = new CheckBoxPreference(activity);
                                item.setKey(String.valueOf(id));
                                item.setTitle("SSID: " + entity.getSsid());
                                item.setSummary("Security: " + entity.getType().name() + " Channel: " + entity.getChannel());
                                item.setPersistent(false);
                                list.addPreference(item);
                                remove.setEnabled(list.getPreferenceCount() > CONST_ITEMS);
                                modify.setEnabled(list.getPreferenceCount() > CONST_ITEMS);

                                if (entity.isDefaultWiFi()) {
                                    Utils.saveWifiConfiguration(activity, entity);
                                    prefs.edit().putString("default.wifi.network", entity.getSsid()).apply();
                                    activity.sendBroadcast(new Intent(TetherIntents.WIFI_DEFAULT_REFRESH));
                                }
                            } else {
                                Toast.makeText(activity, "Add network failed. Please check if SSID name is unique and already on the list", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                dialog.show();
                return false;
            }
        });

        modify.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Preference pref = null;
                int selected = 0;

                for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
                    if (list.getPreference(idx) instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) list.getPreference(idx)).isChecked();
                        if (status) {
                            selected++;
                            pref = list.getPreference(idx);
                        }
                    }
                }

                if (selected != 1) {
                    Toast.makeText(activity, "Please select only one item!", Toast.LENGTH_LONG).show();
                } else {
                    WiFiTethering entity = db.getWifiTethering(Integer.valueOf(pref.getKey()));
                    entity.setDefaultWiFi(prefs.getString("default.wifi.network", "").equals(entity.getSsid()));
                    final WiFiTetheringDialog dialog = new WiFiTetheringDialog(activity, entity);
                    final Preference finalPref = pref;
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dlg) {
                            WiFiTethering entity = dialog.getEntity();
                            if (entity != null) {
                                long id = db.addOrUpdateWiFiTethering(entity);

                                if (id > 0) {
                                    finalPref.setKey(String.valueOf(id));
                                    finalPref.setTitle("SSID: " + entity.getSsid());
                                    finalPref.setSummary("Security: " + entity.getType().name() + " Channel: " + entity.getChannel());
                                    finalPref.setPersistent(false);

                                    if (entity.isDefaultWiFi()) {
                                        Utils.saveWifiConfiguration(activity, entity);
                                        prefs.edit().putString("default.wifi.network", entity.getSsid()).apply();
                                        activity.sendBroadcast(new Intent(TetherIntents.WIFI_DEFAULT_REFRESH));
                                    }
                                } else {
                                    Toast.makeText(activity, "Add network failed. Please check if SSID name is unique and already on the list", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
                    dialog.show();
                }
                return true;
            }

        });

        remove.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean changed = false;

                for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
                    Preference pref = list.getPreference(idx);
                    if (pref instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) pref).isChecked();
                        if (status) {
                            WiFiTethering item = db.getWifiTethering(Integer.valueOf(pref.getKey()));
                            if (db.removeWiFiTethering(Integer.parseInt(pref.getKey())) > 0) {
                                list.removePreference(pref);
                                changed = true;
                                String defaultSsid = prefs.getString("default.wifi.network", "");

                                if (defaultSsid.equals(item.getSsid())) {
                                    prefs.edit().remove("default.wifi.network");
                                    Utils.saveWifiConfiguration(activity, null);
                                    activity.sendBroadcast(new Intent(TetherIntents.WIFI_DEFAULT_REFRESH));
                                }
                            }
                        }
                    }
                }

                remove.setEnabled(list.getPreferenceCount() > CONST_ITEMS);
                modify.setEnabled(list.getPreferenceCount() > CONST_ITEMS);

                if (!changed) {
                    Toast.makeText(activity, "Please select any item", Toast.LENGTH_LONG).show();
                }
                return true;
            }

        });
    }
}
