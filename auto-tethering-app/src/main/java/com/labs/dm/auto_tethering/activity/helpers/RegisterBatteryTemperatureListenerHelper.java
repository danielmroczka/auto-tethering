package com.labs.dm.auto_tethering.activity.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.activity.MainActivity;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static com.labs.dm.auto_tethering.AppProperties.TEMPERATURE_LIMIT;
import static com.labs.dm.auto_tethering.TetherIntents.TEMPERATURE_ABOVE_LIMIT;
import static com.labs.dm.auto_tethering.TetherIntents.TEMPERATURE_BELOW_LIMIT;

/**
 * Created by Daniel Mroczka on 9/21/2016.
 */
public class RegisterBatteryTemperatureListenerHelper extends AbstractRegisterHelper {

    public static final String TEMP_VALUE_STOP = "temp.value.stop";
    public static final String TEMP_VALUE_START = "temp.value.start";
    private float lastTemperature;
    private final BatteryReceiver batteryReceiver;

    public RegisterBatteryTemperatureListenerHelper(MainActivity activity) {
        super(activity);
        batteryReceiver = new BatteryReceiver();
    }

    @Override
    public void registerUIListeners() {
        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue == null || newValue.toString().isEmpty()) {
                    Toast.makeText(activity, "Provide not empty value!", Toast.LENGTH_LONG).show();
                    return false;
                }

                if (TEMP_VALUE_START.equals(preference.getKey())) {
                    if (Integer.parseInt((String) newValue) >= Integer.parseInt(prefs.getString(TEMP_VALUE_STOP, "40"))) {
                        Toast.makeText(activity, "Value should be less than " + prefs.getString(TEMP_VALUE_STOP, "40"), Toast.LENGTH_LONG).show();
                        return false;
                    }
                    preference.setSummary("When temp. returns to: " + newValue + " °C");
                } else if (TEMP_VALUE_STOP.equals(preference.getKey())) {
                    if (Integer.parseInt((String) newValue) <= Integer.parseInt(prefs.getString(TEMP_VALUE_START, "40"))) {
                        Toast.makeText(activity, "Value should be greater than " + prefs.getString(TEMP_VALUE_START, "40"), Toast.LENGTH_LONG).show();
                        return false;
                    }
                    preference.setSummary("When temp. higher than: " + newValue + " °C");
                }
                return true;
            }
        };
        batteryReceiver.register(activity, new IntentFilter(ACTION_BATTERY_CHANGED));
        EditTextPreference tempStart = getEditTextPreference(TEMP_VALUE_START);
        tempStart.setOnPreferenceChangeListener(changeListener);
        tempStart.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(0, 100)});
        EditTextPreference tempStop = getEditTextPreference(TEMP_VALUE_STOP);
        tempStop.setOnPreferenceChangeListener(changeListener);
        tempStop.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(0, 100)});

        CheckBoxPreference checkBoxPreference = getCheckBoxPreference("temp.monitoring.enable");
        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!((Boolean) newValue)) {
                    activity.sendBroadcast(new Intent(TEMPERATURE_BELOW_LIMIT));
                }
                return true;
            }
        });
    }

    @Override
    public void unregisterUIListeners() {
        batteryReceiver.unregister(activity);
    }

    private class BatteryReceiver extends BroadcastReceiver {
        boolean isRegistered;

        Intent register(Context context, IntentFilter filter) {
            isRegistered = true;
            return context.registerReceiver(this, filter);
        }

        boolean unregister(Context context) {
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
            if (lastTemperature < temperature) {
                sign = "↑";
            } else if (lastTemperature > temperature) {
                sign = "↓";
            }
            final PreferenceScreen current = getPreferenceScreen("temp.current");

            Spannable summary = new SpannableString(String.format("%.1f°C %s", temperature, sign));
            summary.setSpan(new ForegroundColorSpan(temperature > TEMPERATURE_LIMIT ? Color.RED : Color.GREEN), 0, summary.length(), 0);
            current.setSummary(summary);

            if (prefs.getBoolean("temp.monitoring.enable", false)) {
                int start = Integer.parseInt(prefs.getString(TEMP_VALUE_START, "50"));
                int stop = Integer.parseInt(prefs.getString(TEMP_VALUE_STOP, "40"));

                if (temperature >= stop) {
                    activity.sendBroadcast(new Intent(TEMPERATURE_ABOVE_LIMIT));
                } else if (temperature <= start) {
                    activity.sendBroadcast(new Intent(TEMPERATURE_BELOW_LIMIT));
                }
                MyLog.d("Temp. monitor", temperature + sign);
            }
            lastTemperature = temperature;
        }
    }

}
