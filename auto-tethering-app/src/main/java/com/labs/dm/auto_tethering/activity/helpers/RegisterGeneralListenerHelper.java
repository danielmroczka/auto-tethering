package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.BatteryManager;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
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

import java.util.Map;

import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_KEEP_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.RETURN_TO_PREV_STATE;
import static com.labs.dm.auto_tethering.AppProperties.SSID;
import static com.labs.dm.auto_tethering.TetherIntents.TEMP_BELOW;
import static com.labs.dm.auto_tethering.TetherIntents.TEMP_OVER;
import static com.labs.dm.auto_tethering.activity.MainActivity.ON_CHANGE_SSID;

/**
 * Created by Daniel Mroczka on 9/13/2016.
 */
public class RegisterGeneralListenerHelper {
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final ServiceHelper serviceHelper;

    private static RegisterGeneralListenerHelper instance;

    public synchronized static RegisterGeneralListenerHelper getInstance(MainActivity activity, SharedPreferences prefs) {
        if (instance == null) {
            instance = new RegisterGeneralListenerHelper(activity, prefs);
        }

        return instance;
    }

    private RegisterGeneralListenerHelper(MainActivity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
        this.serviceHelper = new ServiceHelper(activity);
        batteryReceiver.register(activity, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void registerListeners() {
        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        };

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

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Preference p = activity.findPreference(entry.getKey());

            switch (entry.getKey()) {
                case IDLE_3G_OFF_TIME:
                case IDLE_TETHERING_OFF_TIME:
                case "temp.value.stop":
                case "temp.value.start":
                case "usb.off.battery.lvl.value":
                case "data.limit.value":
                    p.setSummary((CharSequence) entry.getValue());
                    p.getEditor().commit();
                    break;

                case SSID:
                    p.setSummary(serviceHelper.getTetheringSSID());
                    break;
            }
        }

        Preference p = activity.findPreference(SSID);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activity.startActivityForResult(preference.getIntent(), ON_CHANGE_SSID);
                return true;
            }
        });

        /*PreferenceScreen usbTethering = (PreferenceScreen) activity.findPreference("usb.tethering");
        usbTethering.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                serviceHelper.usbTethering(true);
                return false;
            }
        });*/

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

        EditTextPreference tempStart = (EditTextPreference) activity.findPreference("temp.value.start");
        tempStart.setOnPreferenceChangeListener(changeListener);
        tempStart.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(0, 100)});
        EditTextPreference tempStop = (EditTextPreference) activity.findPreference("temp.value.stop");
        tempStop.setOnPreferenceChangeListener(changeListener);
        tempStop.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(0, 100)});

        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) activity.findPreference("temp.monitoring.enable");
        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue == false) {
                    activity.sendBroadcast(new Intent(TEMP_BELOW));
                }
                return false;
            }
        });
    }

    private void startService() {
        if (!serviceHelper.isServiceRunning(TetheringService.class)) {
            Intent serviceIntent = new Intent(activity, TetheringService.class);
            serviceIntent.putExtra("runFromActivity", true);
            activity.startService(serviceIntent);
        }
    }

    private float lastTemperature;

    private BatteryReceiver batteryReceiver = new BatteryReceiver();

    public void unregisterListener() {
        batteryReceiver.unregister(activity);
    }

    private class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (min <= input && input <= max || (dest.length() + source.length() < 2)) {
                    return null;
                }
            } catch (NumberFormatException nfe) {
                Log.e("InputFilterMinMax", nfe.getMessage());
            }
            return "";
        }
    }

    private class BatteryReceiver extends BroadcastReceiver {

        public boolean isRegistered;

        public Intent register(Context context, IntentFilter filter) {
            isRegistered = true;
            return context.registerReceiver(this, filter);
        }

        public boolean unregister(Context context) {
            if (isRegistered) {
                context.unregisterReceiver(this);
                isRegistered = false;
                return true;
            }
            return false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            float temperature = (float) (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10);
            String sign = "→";
            if (lastTemperature > temperature) {
                sign = "↓";
            } else if (lastTemperature < temperature) {
                sign = "↑";
            }
            final PreferenceScreen current = (PreferenceScreen) activity.findPreference("temp.current");

            Spannable summary = new SpannableString(String.format("%.1f°C %s", temperature, sign));
            summary.setSpan(new ForegroundColorSpan(temperature > 40 ? Color.RED : Color.GREEN), 0, summary.length(), 0);
            current.setSummary(summary);

            if (prefs.getBoolean("temp.monitoring.enable", false)) {
                int start = Integer.parseInt(prefs.getString("temp.value.start", "50"));
                int stop = Integer.parseInt(prefs.getString("temp.value.stop", "40"));

                if (temperature >= stop) {
                    activity.sendBroadcast(new Intent(TEMP_OVER));
                } else if (temperature <= start) {
                    activity.sendBroadcast(new Intent(TEMP_BELOW));
                }
                Log.d("Temp. monitor", temperature + sign);
            }
            lastTemperature = temperature;
        }
    }
}
