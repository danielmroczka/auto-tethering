package com.labs.dm.auto_tethering.activity.helpers;

import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

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
            Preference item = new CheckBoxPreference(activity);
            item.setKey(String.valueOf(net.getId()));
            item.setTitle("SSID: " + net.getSsid());
            item.setSummary("Security: " + net.getType().name() + " Channel: " + net.getChannel());
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
                        long id = db.addOrUpdateWiFiTethering(entity);

                        if (id > -1) {
                            Preference item = new CheckBoxPreference(activity);
                            item.setKey(String.valueOf(id));
                            item.setTitle("SSID: " + entity.getSsid());
                            item.setSummary("Security: " + entity.getType().name() + " Channel: " + entity.getChannel());
                            list.addPreference(item);
                            remove.setEnabled(list.getPreferenceCount() > 2);
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

    void load() {

    }

}
