package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

import static com.labs.dm.auto_tethering.AppProperties.*;
import static com.labs.dm.auto_tethering.activity.MainActivity.ON_CHANGE_SSID;

/**
 * Created by Daniel Mroczka on 9/13/2016.
 */
public class RegisterGeneralListenerHelper extends AbstractRegisterHelper {
    private final ServiceHelper serviceHelper;

    private static RegisterGeneralListenerHelper instance;

    public synchronized static RegisterGeneralListenerHelper getInstance(MainActivity activity) {
        if (instance == null) {
            instance = new RegisterGeneralListenerHelper(activity);
        }

        return instance;
    }

    private RegisterGeneralListenerHelper(MainActivity activity) {
        super(activity);
        this.serviceHelper = new ServiceHelper(activity);
    }

    public void registerUIListeners() {
        Preference.OnPreferenceChangeListener revertStateCheckBoxListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {

                    Toast toast = Toast.makeText(activity, "Once application has been closed tethering and internet connection state will be restored to state before open this application", Toast.LENGTH_LONG);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    if (v != null) {
                        v.setGravity(Gravity.CENTER);
                        v.setPadding(12, 12, 12, 12);
                    }
                    toast.show();
                }
                return true;
            }
        };

        PreferenceScreen editSSID = (PreferenceScreen) activity.findPreference(SSID);
        editSSID.setOnPreferenceChangeListener(changeListener);

        EditTextPreference tetheringIdleTime = (EditTextPreference) activity.findPreference(IDLE_TETHERING_OFF_TIME);
        tetheringIdleTime.setOnPreferenceChangeListener(changeListener);
        EditTextPreference internetIdleTime = (EditTextPreference) activity.findPreference(IDLE_3G_OFF_TIME);
        internetIdleTime.setOnPreferenceChangeListener(changeListener);

        CheckBoxPreference revertStateCheckBox = (CheckBoxPreference) activity.findPreference(RETURN_TO_PREV_STATE);
        revertStateCheckBox.setOnPreferenceChangeListener(revertStateCheckBoxListener);

        final PreferenceScreen connectedClients = (PreferenceScreen) activity.findPreference("idle.connected.clients");

        connectedClients.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                connectedClients.setTitle("Connected clients: " + Utils.connectedClients());
                return false;
            }
        });

        final CheckBoxPreference activationStartup = (CheckBoxPreference) activity.findPreference("activate.on.startup");
        final ComponentName componentName = new ComponentName(activity, BootCompletedReceiver.class);
        int state = activity.getPackageManager().getComponentEnabledSetting(componentName);

        activationStartup.setChecked(state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

        activationStartup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                int state = activity.getPackageManager().getComponentEnabledSetting(componentName);

                if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                    activity.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    Toast.makeText(activity, R.string.on_startup_enable, Toast.LENGTH_LONG).show();
                } else {
                    activity.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    Toast.makeText(activity, R.string.on_startup_disable, Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });

        CheckBoxPreference keepServiceCheckBox = (CheckBoxPreference) activity.findPreference(ACTIVATE_KEEP_SERVICE);
        keepServiceCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    startService();
                }
                return true;
            }
        });

        CheckBoxPreference roamingCheckBox = (CheckBoxPreference) activity.findPreference("activate.on.roaming");
        roamingCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue && !Utils.isDataRoamingEnabled(activity)) {

                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.warning)
                            .setMessage("Current system setting disables Data Roaming.\nYou must also enable it!\n\nDo you want to do it now?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setAction(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
                                    activity.startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.no, null
                            ).show();
                }
                return true;
            }
        });

        EditTextPreference batteryLevelValue = (EditTextPreference) activity.findPreference("usb.off.battery.lvl.value");
        batteryLevelValue.setOnPreferenceChangeListener(changeListener);
        batteryLevelValue.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(0, 100)});

        Preference p = activity.findPreference(SSID);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activity.startActivityForResult(preference.getIntent(), ON_CHANGE_SSID);
                return true;
            }
        });

        PreferenceScreen resetDataUsage = (PreferenceScreen) activity.findPreference("data.limit.reset");
        resetDataUsage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.warning)
                        .setMessage("Do you want to reset data usage counter?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                long dataUsage = ServiceHelper.getDataUsage();
                                prefs.edit().putLong("data.usage.reset.value", dataUsage).apply();
                                prefs.edit().putLong("data.usage.last.value", dataUsage).apply();
                                prefs.edit().putLong("data.usage.reset.timestamp", System.currentTimeMillis()).apply();

                                Intent intent = new Intent(TetherIntents.DATA_USAGE);
                                activity.sendBroadcast(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null
                        ).show();

                return true;
            }
        });

        EditTextPreference dataLimit = (EditTextPreference) activity.findPreference("data.limit.value");
        dataLimit.setOnPreferenceChangeListener(changeListener);

        final CheckBoxPreference btCheckBox = (CheckBoxPreference) activity.findPreference("bt.start.discovery");
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

    private void startService() {
        if (!serviceHelper.isServiceRunning(TetheringService.class)) {
            Intent serviceIntent = new Intent(activity, TetheringService.class);
            serviceIntent.putExtra("runFromActivity", true);
            activity.startService(serviceIntent);
        }
    }


}
