package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Bluetooth;
import com.labs.dm.auto_tethering.service.ServiceHelper;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.labs.dm.auto_tethering.AppProperties.MAX_BT_DEVICES;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class RegisterBluetoothListenerHelper extends AbstractRegisterHelper {

    public RegisterBluetoothListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        getPreferenceScreen("screen.bluetooth").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prepareBTList();
                return true;
            }
        });

        getPreferenceScreen("bt.add.device").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final List<Bluetooth> devices = db.readBluetooth();
                        if (devices.size() >= MAX_BT_DEVICES) {
                            Toast.makeText(activity, "Exceeded the limit of max. " + MAX_BT_DEVICES + " devices!", Toast.LENGTH_LONG).show();
                            return false;
                        }

                        final PreferenceCategory category = getPreferenceCategory("bt.list");

                        AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
                        builderSingle.setIcon(R.drawable.ic_bluetooth);
                        builderSingle.setTitle("Select one item");

                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_singlechoice);

                        final Set<BluetoothDevice> pairedDevices = new ServiceHelper(activity).getBondedDevices(false);
                        for (BluetoothDevice device : pairedDevices) {
                            arrayAdapter.add(device.getName());
                        }

                        builderSingle.setPositiveButton("Open pair dialog", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                                activity.startActivity(intent);
                            }
                        });

                        builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        buildAdapter(dialog, which, arrayAdapter, devices, pairedDevices, category);
                                    }
                                }
                        );
                        builderSingle.show();
                        return false;
                    }
                }
        );

        final PreferenceScreen remove = getPreferenceScreen("bt.remove.device");
        remove.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        PreferenceCategory p = getPreferenceCategory("bt.list");
                        boolean changed = false;

                        for (int idx = p.getPreferenceCount() - 1; idx >= 0; idx--) {
                            Preference pref = p.getPreference(idx);
                            if (pref instanceof CheckBoxPreference) {
                                boolean status = ((CheckBoxPreference) pref).isChecked();
                                if (status) {
                                    if (db.removeBluetooth(Integer.parseInt(pref.getKey())) > 0) {
                                        p.removePreference(pref);
                                        changed = true;
                                    }
                                }
                            }
                        }

                        remove.setEnabled(p.getPreferenceCount() > 2);

                        if (!changed) {
                            Toast.makeText(activity, "Please select any item", Toast.LENGTH_LONG).show();
                        }
                        return true;
                    }
                }
        );

        final CheckBoxPreference btCheckBox = getCheckBoxPreference("bt.start.discovery");
        btCheckBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!btCheckBox.isChecked()) {
                    activity.sendBroadcast(new Intent(TetherIntents.BT_RESTORE));
                } else {
                    Toast.makeText(activity, "You might be asked to approve Bluetooth connection on some preferred devices.", Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });
        btCheckBox.setChecked(prefs.getBoolean("bt.start.discovery", false));
    }

    private void buildAdapter(DialogInterface dialog, int which, ArrayAdapter<String> arrayAdapter, List<Bluetooth> devices, Set<BluetoothDevice> pairedDevices, PreferenceCategory category) {
        if (which >= 0) {
            String name = arrayAdapter.getItem(which);

            boolean found = false;
            for (Bluetooth b : devices) {
                if (name != null && name.equals(b.getName())) {
                    found = true;
                }
            }

            if (found) {
                Toast.makeText(activity, "Device " + name + " is already added!", Toast.LENGTH_LONG).show();
            } else {
                Bluetooth bluetooth = null;
                for (BluetoothDevice device : pairedDevices) {
                    if (name != null && name.equals(device.getName())) {
                        bluetooth = new Bluetooth(name, device.getAddress());
                    }
                }

                if (bluetooth != null) {
                    long id = db.addOrUpdateBluetooth(bluetooth);
                    if (id > 0) {
                        Preference ps = new CheckBoxPreference(activity);
                        ps.setTitle(name);
                        ps.setSummary((bluetooth.getAddress() != null ? bluetooth.getAddress() : "") + (bluetooth.getUsed() > 0 ? "\nConnected on:" + new Date(bluetooth.getUsed()) : ""));
                        ps.setKey(String.valueOf(id));
                        ps.setPersistent(false);
                        category.addPreference(ps);
                    }
                }
                getPreferenceScreen("bt.remove.device").setEnabled(category.getPreferenceCount() > 2);
            }
        }
        dialog.dismiss();
    }

    private void prepareBTList() {
        clean();
        PreferenceCategory pc = getPreferenceCategory("bt.list");
        Set<BluetoothDevice> bondedDevices = new ServiceHelper(activity).getBondedDevices(false);
        List<Bluetooth> preferredDevices = db.readBluetooth();

        for (Bluetooth device : preferredDevices) {
            if (device != null && !TextUtils.isEmpty(device.getName())) {
                Preference ps = new CheckBoxPreference(activity);
                ps.setTitle(device.getName());
                ps.setSummary((device.getAddress() != null ? device.getAddress() : "") + (device.getUsed() > 0 ? "\nConnected on:" + new Date(device.getUsed()) : ""));
                ps.setKey(String.valueOf(device.getId()));
                ps.setPersistent(false);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    boolean found = false;
                    for (BluetoothDevice bd : bondedDevices) {
                        if (device.getName().equals(bd.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        ps.setSummary("Device is no longer paired!");
                    }
                }

                pc.addPreference(ps);
            }
        }

        getPreferenceScreen("bt.remove.device").setEnabled(pc.getPreferenceCount() > 2);
    }

    private void clean() {
        PreferenceCategory p = getPreferenceCategory("bt.list");
        for (int idx = p.getPreferenceCount() - 1; idx >= 0; idx--) {
            Preference pref = p.getPreference(idx);
            if (pref instanceof CheckBoxPreference) {
                p.removePreference(pref);
            }
        }
    }
}

