package com.labs.dm.auto_tethering.activity;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.service.TetheringService;

import java.util.Map;

/**
 * Created by Daniel Mroczka
 */
public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int ON_CHANGE_SSID = 123;
    private SharedPreferences prefs;
    private Notification notification;
    private CharSequence contentText;
    private PendingIntent contentIntent;
    private NotificationManager notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        loadPrefs();
        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        };
        Preference.OnPreferenceChangeListener editTimeChangeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean res = Utils.validateTime((String) newValue);
                if (res) {
                    preference.setSummary((String) newValue);
                }
                return res;
            }
        };

        PreferenceScreen editSSID = (PreferenceScreen) findPreference(AppProperties.SSID);
        editSSID.setOnPreferenceChangeListener(changeListener);
        EditTextPreference editTimeOn = (EditTextPreference) findPreference(AppProperties.TIME_ON);
        editTimeOn.setOnPreferenceChangeListener(editTimeChangeListener);
        EditTextPreference editTimeOff = (EditTextPreference) findPreference(AppProperties.TIME_OFF);
        editTimeOff.setOnPreferenceChangeListener(editTimeChangeListener);

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Preference p = findPreference(entry.getKey());

            if (AppProperties.TIME_ON.equals(entry.getKey()) || AppProperties.TIME_OFF.equals(entry.getKey())) {
                p.setSummary((CharSequence) entry.getValue());
            }

            if (AppProperties.SSID.equals(entry.getKey())) {
                WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
                p.setSummary(cfg != null ? cfg.SSID : "<empty>");
            }
        }

        Preference p = findPreference(AppProperties.SSID);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(preference.getIntent(), ON_CHANGE_SSID);
                return true;
            }
        });

        //int icon = R.drawable.wifi;
        //CharSequence tickerText = "Wifi";
        //long when = System.currentTimeMillis();
        //notification(icon, tickerText, when);
        setupSummaryText1();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void loadPrefs() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == ON_CHANGE_SSID) {
            Preference p = findPreference(AppProperties.SSID);
            WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
            p.setSummary(cfg.SSID);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this, TetheringService.class);
        startService(serviceIntent);
        WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
        prefs.edit().putString(AppProperties.SSID, cfg.SSID).apply();
        loadPrefs();
        displayPrompt();
    }

    private void displayPrompt() {
        if (!prefs.getString(AppProperties.LATEST_VERSION, "").isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Warning");
        builder.setMessage(getString(R.string.prompt));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(AppProperties.ACTIVATE_3G, true).apply();
                prefs.edit().putBoolean(AppProperties.ACTIVATE_TETHERING, true).apply();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(AppProperties.ACTIVATE_3G, false).apply();
                prefs.edit().putBoolean(AppProperties.ACTIVATE_TETHERING, false).apply();
            }
        });


        AlertDialog alert = builder.create();
        alert.show();
        prefs.edit().putString(AppProperties.LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference p = findPreference(key);

        switch (key) {
            case AppProperties.ACTIVATE_ON_SIMCARD: {
                if (sharedPreferences.getBoolean(AppProperties.ACTIVATE_ON_SIMCARD, false)) {
                    TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    String simCard = tMgr.getSimSerialNumber();
                    if (simCard == null || simCard.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Unable to retrieve SIM Card Serial!", Toast.LENGTH_LONG).show();
                        prefs.edit().putBoolean(AppProperties.ACTIVATE_ON_SIMCARD, false).apply();
                        prefs.edit().putString(AppProperties.SIMCARD, "").apply();
                    } else {
                        prefs.edit().putString(AppProperties.SIMCARD, simCard);
                    }
                }
            }

            case AppProperties.TIME_OFF:
            case AppProperties.TIME_ON: {
                loadPrefs();
                break;
            }

            case AppProperties.ACTIVATE_3G:
            case AppProperties.ACTIVATE_TETHERING:
            case AppProperties.ACTIVATE_ON_STARTUP: {
                ((CheckBoxPreference) findPreference(key)).setChecked(sharedPreferences.getBoolean(key, false));
                break;
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    private void notification(int icon, CharSequence tickerText, long when) {
        notification = new Notification(icon, tickerText, when);

        Context context = getApplicationContext();
        contentText = "Auto WIFI Tethering";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        contentIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        notification.setLatestEventInfo(context, "",
                contentText, contentIntent);
        notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    void setupSummaryText1() {
        PreferenceScreen ps = (PreferenceScreen) findPreference("scheduler.screen");
        StringBuilder sb = new StringBuilder();
        if (prefs.getBoolean(AppProperties.SCHEDULER, false)) {
            sb.append("Scheduler enabled between " + prefs.getString(AppProperties.TIME_OFF, "0:00") + " and " + prefs.getString(AppProperties.TIME_ON, "6:00"));
        } else {
            sb.append("Scheduler disabled");
        }
        ps.setSummary(sb.toString());

        ps = (PreferenceScreen) findPreference("startup.screen");
        ps.setSummary("Startup activity settings");

        ps = (PreferenceScreen) findPreference("adv.act.screen");
        ps.setSummary("Advanced activity settings");


    }

}
