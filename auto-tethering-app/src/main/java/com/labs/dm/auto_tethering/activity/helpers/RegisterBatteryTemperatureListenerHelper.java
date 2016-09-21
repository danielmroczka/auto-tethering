package com.labs.dm.auto_tethering.activity.helpers;

import android.content.*;
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
import android.util.Log;
import com.labs.dm.auto_tethering.activity.MainActivity;

import static com.labs.dm.auto_tethering.AppProperties.TEMPERATURE_LIMIT;
import static com.labs.dm.auto_tethering.TetherIntents.TEMPEARTURE_BELOW_LIMIT;
import static com.labs.dm.auto_tethering.TetherIntents.TEMPERATURE_ABOVE_LIMIT;

/**
 * Created by Daniel Mroczka on 9/21/2016.
 */
public class RegisterBatteryTemperatureListenerHelper extends AbstractRegisterHelper {

    private float lastTemperature;
    private final BatteryReceiver batteryReceiver;

    private static RegisterBatteryTemperatureListenerHelper instance;

    public synchronized static RegisterBatteryTemperatureListenerHelper getInstance(MainActivity activity, SharedPreferences prefs) {
        if (instance == null) {
            instance = new RegisterBatteryTemperatureListenerHelper(activity);
        }

        return instance;
    }

    private RegisterBatteryTemperatureListenerHelper(MainActivity activity) {
        super(activity);
        batteryReceiver = new BatteryReceiver();
    }

    public void registerListener() {
        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        };
        batteryReceiver.register(activity, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
                    activity.sendBroadcast(new Intent(TEMPEARTURE_BELOW_LIMIT));
                }
                return false;
            }
        });
    }

    public void unregisterListener() {
        batteryReceiver.unregister(activity);
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
            summary.setSpan(new ForegroundColorSpan(temperature > TEMPERATURE_LIMIT ? Color.RED : Color.GREEN), 0, summary.length(), 0);
            current.setSummary(summary);

            if (prefs.getBoolean("temp.monitoring.enable", false)) {
                int start = Integer.parseInt(prefs.getString("temp.value.start", "50"));
                int stop = Integer.parseInt(prefs.getString("temp.value.stop", "40"));

                if (temperature >= stop) {
                    activity.sendBroadcast(new Intent(TEMPERATURE_ABOVE_LIMIT));
                } else if (temperature <= start) {
                    activity.sendBroadcast(new Intent(TEMPEARTURE_BELOW_LIMIT));
                }
                Log.d("Temp. monitor", temperature + sign);
            }
            lastTemperature = temperature;
        }
    }

}
