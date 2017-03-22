package com.labs.dm.auto_tethering.activity.helpers;

import android.content.DialogInterface;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.WiFiTethering;
import com.labs.dm.auto_tethering.ui.dialog.WiFiTetheringDialog;

import java.util.List;

/**
 * Created by Daniel Mroczka on 21-Mar-17.
 */

public class RegisterWiFiListListenerHelper extends AbstractRegisterHelper {

    public RegisterWiFiListListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        final PreferenceCategory list = getPreferenceCategory("wifi.list");
        final PreferenceScreen remove = getPreferenceScreen("wifi.remove.device");

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

        remove.setEnabled(list.getPreferenceCount() > 2);

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

                            if (id > -1) {
                                Preference item = new CheckBoxPreference(activity);
                                item.setKey(String.valueOf(id));
                                item.setTitle("SSID: " + entity.getSsid());
                                item.setSummary("Security: " + entity.getType().name() + " Channel: " + entity.getChannel());
                                item.setPersistent(false);
                                list.addPreference(item);
                                remove.setEnabled(list.getPreferenceCount() > 2);

                                if (entity.isDefaultWiFi()) {
                                    prefs.edit().putString("default.wifi.network", entity.getSsid()).apply();
                                    activity.sendBroadcast(new Intent(TetherIntents.WIFI_DEFAULT_REFRESH));
                                }
                            }
                        }
                    }
                });
                dialog.show();
                return false;
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
                            if (db.removeWiFiTethering(Integer.parseInt(pref.getKey())) > 0) {
                                list.removePreference(pref);
                                changed = true;
                                String defaultSsid = prefs.getString("default.wifi.network", null);
                                List<WiFiTethering> list = db.readWiFiTethering();
                                for (WiFiTethering item : list) {
                                    if (pref.getKey().equals(item.getId())) {
                                        if (defaultSsid != null && defaultSsid.equals(item.getSsid())) {
                                            prefs.edit().remove("default.wifi.network");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                remove.setEnabled(list.getPreferenceCount() > 2);

                if (!changed) {
                    Toast.makeText(activity, "Please select any item", Toast.LENGTH_LONG).show();
                }
                return true;
            }

        });
    }
}
