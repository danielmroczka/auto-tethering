package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.labs.dm.auto_tethering.receiver.BluetoothBroadcastReceiver;
import com.labs.dm.auto_tethering.service.ServiceHelper;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.labs.dm.auto_tethering.AppProperties.MAX_BT_DEVICES;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */
public class RegisterBluetoothListenerHelper extends AbstractRegisterHelper {

    private boolean activated;

    public RegisterBluetoothListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        final ServiceHelper serviceHelper = new ServiceHelper(activity);

        getPreferenceScreen("screen.bluetooth").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prepareBTList();
                return false;
            }
        });

        final PreferenceCategory list = getPreferenceCategory("bt.list");

        getPreferenceScreen("bt.add.device").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final List<Bluetooth> devices = db.readBluetooth();
                        if (devices.size() >= MAX_BT_DEVICES) {
                            Toast.makeText(activity, "Exceeded the limit of max. " + MAX_BT_DEVICES + " devices!", Toast.LENGTH_LONG).show();
                            return false;
                        }

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
                                        buildAdapter(dialog, which, arrayAdapter, devices, pairedDevices, list);
                                        if (!prefs.getBoolean("bt.incoming.listen", false)) {
                                            Toast.makeText(activity, "You have successfully added device but you haven't selected 'Activate thethering once Bluetooth..'\nUntil this option is not selected, background service won't find Bluetooth devices!", Toast.LENGTH_LONG).show();
                                        }
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
                        boolean changed = false;

                        for (int idx = list.getPreferenceCount() - 1; idx >= 0; idx--) {
                            Preference pref = list.getPreference(idx);
                            if (pref instanceof CheckBoxPreference) {
                                boolean status = ((CheckBoxPreference) pref).isChecked();
                                if (status) {
                                    if (db.removeBluetooth(Integer.parseInt(pref.getKey())) > 0) {
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
                }
        );

        final CheckBoxPreference btCheckBox = getCheckBoxPreference("bt.incoming.listen");
        btCheckBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!btCheckBox.isChecked()) {
                    activity.sendBroadcast(new Intent(TetherIntents.BT_STOP));
                } else {
                    activity.sendBroadcast(new Intent(TetherIntents.BT_START_TASKSEARCH));
                    Toast.makeText(activity, "You might be asked to approve Bluetooth connection on some preferred devices.", Toast.LENGTH_LONG).show();
                    if (db.readBluetooth().isEmpty()) {
                        Toast.makeText(activity, "List of configured devices is empty.\nBluetooth activation will not work until you add any device!", Toast.LENGTH_LONG).show();
                    }
                }

                return true;
            }
        });
        btCheckBox.setChecked(prefs.getBoolean("bt.incoming.listen", false));

        final CheckBoxPreference listenCheckBox = getCheckBoxPreference("bt.incoming.listen");
        listenCheckBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final ComponentName componentName = new ComponentName(activity, BluetoothBroadcastReceiver.class);

                if (listenCheckBox.isChecked()) {
                    activity.getApplicationContext().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    Toast.makeText(activity, "Listen to Bluetooth Connection activated", Toast.LENGTH_LONG).show();

                    if (!serviceHelper.isBluetoothActive()) {
                        serviceHelper.setBluetoothStatus(true);
                        activated = true;
                    }
                } else {
                    activity.getApplicationContext().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    if (activated && serviceHelper.isBluetoothActive()) {
                        serviceHelper.setBluetoothStatus(false);
                        activated = false;
                    }
                }

                return true;
            }
        });
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
        clean("bt.list");
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
}

