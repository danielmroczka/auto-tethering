package com.labs.dm.auto_tethering.activity;

import android.app.AlertDialog;
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

import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_SIMCARD;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_STARTUP;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.LATEST_VERSION;
import static com.labs.dm.auto_tethering.AppProperties.SIMCARD_LIST;
import static com.labs.dm.auto_tethering.AppProperties.SSID;
import static com.labs.dm.auto_tethering.AppProperties.TIME_OFF;
import static com.labs.dm.auto_tethering.AppProperties.TIME_ON;

/**
 * Created by Daniel Mroczka
 */
public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int ON_CHANGE_SSID = 1;
    private SharedPreferences prefs;

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

        PreferenceScreen editSSID = (PreferenceScreen) findPreference(SSID);
        editSSID.setOnPreferenceChangeListener(changeListener);
        EditTextPreference editTimeOn = (EditTextPreference) findPreference(TIME_ON);
        editTimeOn.setOnPreferenceChangeListener(editTimeChangeListener);
        EditTextPreference editTimeOff = (EditTextPreference) findPreference(TIME_OFF);
        editTimeOff.setOnPreferenceChangeListener(editTimeChangeListener);

        EditTextPreference tetheringIdleTime = (EditTextPreference) findPreference(IDLE_TETHERING_OFF_TIME);
        tetheringIdleTime.setOnPreferenceChangeListener(changeListener);
        EditTextPreference internetIdleTime = (EditTextPreference) findPreference(IDLE_3G_OFF_TIME);
        internetIdleTime.setOnPreferenceChangeListener(changeListener);

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Preference p = findPreference(entry.getKey());

            if (TIME_ON.equals(entry.getKey()) || TIME_OFF.equals(entry.getKey()) || IDLE_TETHERING_OFF_TIME.equals(entry.getKey()) || IDLE_3G_OFF_TIME.equals(entry.getKey())) {
                p.setSummary((CharSequence) entry.getValue());
            }

            if (SSID.equals(entry.getKey())) {
                WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
                p.setSummary(cfg != null ? cfg.SSID : "<empty>");
            }
        }

        Preference p = findPreference(SSID);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(preference.getIntent(), ON_CHANGE_SSID);
                return true;
            }
        });

        Preference button = findPreference("reset");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Warning");
                builder.setMessage(getString(R.string.promptReset));

                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().clear().apply();
                        //loadPrefs();
                    }
                });

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });


                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });

        final Preference simedit = findPreference("edit.simcard");
        final String serials = prefs.getString(AppProperties.SIMCARD_LIST, "");
        TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        final String simCard = tMgr.getSimSerialNumber();
        if (Utils.exists(serials, simCard)) {
            simedit.setTitle(R.string.remove_simcard_title);
            simedit.setSummary(R.string.remove_simcard_summary);
        } else {
            simedit.setTitle(R.string.add_simcard_title);
            simedit.setSummary(R.string.add_simcard_summary);
        }

        simedit.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String res = prefs.getString(AppProperties.SIMCARD_LIST, "");
                if (Utils.exists(res, simCard)) {
                    res = Utils.remove(res, simCard);
                    simedit.setTitle(R.string.add_simcard_title);
                    simedit.setSummary(R.string.add_simcard_summary);
                    if (prefs.getBoolean(AppProperties.ACTIVATE_ON_SIMCARD, false)) {
                        Toast.makeText(getApplicationContext(), R.string.simcard_warn, Toast.LENGTH_LONG).show();
                    }
                } else {
                    res = Utils.add(res, simCard);
                    simedit.setTitle(R.string.remove_simcard_title);
                    simedit.setSummary(R.string.remove_simcard_summary);
                }

                prefs.edit().putString(SIMCARD_LIST, res).apply();

                return false;
            }
        });
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
            Preference p = findPreference(SSID);
            WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
            p.setSummary(cfg != null ? cfg.SSID : null);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this, TetheringService.class);
        startService(serviceIntent);
        WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
        prefs.edit().putString(SSID, cfg != null ? cfg.SSID : null).apply();
        loadPrefs();
        displayPrompt();
    }

    private void displayPrompt() {
        if (!prefs.getString(LATEST_VERSION, "").isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.warning);
        builder.setMessage(getString(R.string.prompt));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(ACTIVATE_3G, true).apply();
                prefs.edit().putBoolean(ACTIVATE_TETHERING, true).apply();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(ACTIVATE_3G, false).apply();
                prefs.edit().putBoolean(ACTIVATE_TETHERING, false).apply();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_exit:
                Intent serviceIntent = new Intent(this, TetheringService.class);
                stopService(serviceIntent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case ACTIVATE_ON_SIMCARD: {
                if (sharedPreferences.getBoolean(ACTIVATE_ON_SIMCARD, false)) {
                    TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    String simCard = tMgr.getSimSerialNumber();
                    if (simCard == null || simCard.isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.cannot_read_simcard, Toast.LENGTH_LONG).show();
                        prefs.edit().putBoolean(ACTIVATE_ON_SIMCARD, false).apply();
                    } else {
                        String simCardWhiteList = prefs.getString(SIMCARD_LIST, "");
                        simCardWhiteList = Utils.add(simCardWhiteList, simCard);
                        prefs.edit().putString(SIMCARD_LIST, simCardWhiteList).apply();
                    }
                }
            }

            case TIME_OFF:
            case TIME_ON:
            case IDLE_3G_OFF_TIME:
            case IDLE_TETHERING_OFF_TIME: {
                loadPrefs();
                break;
            }

            case ACTIVATE_3G:
            case ACTIVATE_TETHERING:
            case ACTIVATE_ON_STARTUP: {
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
}
