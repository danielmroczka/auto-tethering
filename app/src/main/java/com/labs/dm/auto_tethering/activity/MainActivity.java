package com.labs.dm.auto_tethering.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.DBManager;
import com.labs.dm.auto_tethering.db.SimCard;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

import java.util.List;
import java.util.Map;

import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_STARTUP;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.LATEST_VERSION;
import static com.labs.dm.auto_tethering.AppProperties.SSID;
import static com.labs.dm.auto_tethering.AppProperties.TIME_OFF;
import static com.labs.dm.auto_tethering.AppProperties.TIME_ON;

/**
 * Created by Daniel Mroczka
 */
public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int ON_CHANGE_SSID = 1;
    private SharedPreferences prefs;
    private ServiceHelper serviceHelper;
    private DBManager db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DBManager.getInstance(getApplicationContext());
        addPreferencesFromResource(R.xml.preferences);
        serviceHelper = new ServiceHelper("ServiceHelper", getApplicationContext());
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

            switch (entry.getKey()) {
                case TIME_ON:
                case TIME_OFF:
                case IDLE_3G_OFF_TIME:
                case IDLE_TETHERING_OFF_TIME:
                    p.setSummary((CharSequence) entry.getValue());
                    break;

                case SSID:
                    p.setSummary(serviceHelper.getTetheringSSID());
                    break;
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

        prepareSimCardWhiteList();
    }

    private void prepareSimCardWhiteList() {
        PreferenceCategory p = (PreferenceCategory) findPreference("simcard.list");
        List<SimCard> list = db.readSimCard();
        for (int idx = 0; idx < p.getPreferenceCount(); idx++) {
            Object object = p.getPreference(idx);
            if (object instanceof CheckBoxPreference) {
                p.removePreference((CheckBoxPreference) object);
            }
        }
        for (SimCard item : list) {
            Preference ps = new CheckBoxPreference(getApplicationContext());
            ps.setTitle(item.getNumber());
            ps.setSummary(item.getSerial());
            p.addPreference(ps);
        }
    }

    private void addSimCard(String number) {
        final TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        final String ssn = tMgr.getSimSerialNumber();
        SimCard simcard = new SimCard(tMgr.getSimSerialNumber(), number, "", 0);
        db.addSimCard(simcard);
        boolean status = db.isOnWhiteList(ssn);
        PreferenceScreen p = (PreferenceScreen) findPreference("add.current.simcard");
        p.setEnabled(!status);
        prepareSimCardWhiteList();
    }

    private void registerAddSimCardListener() {
        final TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        final String ssn = tMgr.getSimSerialNumber();
        boolean status = db.isOnWhiteList(ssn);

        PreferenceScreen p = (PreferenceScreen) findPreference("add.current.simcard");
        p.setEnabled(!status);
        final String[] number = {""};
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                number[0] = tMgr.getLine1Number();
                // TODO:
                if (number[0] == null || number[0].isEmpty()) {
                    LayoutInflater li = LayoutInflater.from(MainActivity.this);
                    final View promptsView = li.inflate(R.layout.add_simcard_prompt, null);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Add phone number")
                            .setMessage("")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setView(promptsView)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
                                    number[0] = userInput.getText().toString();
                                    addSimCard(number[0]);
                                }
                            })
                            .setNegativeButton(R.string.no, null).show();
                    return true;
                } else {

                    addSimCard(number[0]);
                }
                return true;
            }
        });

        PreferenceScreen p2 = (PreferenceScreen) findPreference("remove.simcard");
        p2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                PreferenceCategory p = (PreferenceCategory) findPreference("simcard.list");
                boolean changed = false;
                for (int idx = 0; idx < p.getPreferenceCount(); idx++) {
                    Object object = p.getPreference(idx);
                    if (object instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) object).isChecked();
                        if (status) {
                            db.removeSimCard(((CheckBoxPreference) object).getSummary().toString());
                            p.removePreference((Preference) object);
                            changed = true;
                        }
                    }
                }

                if (!changed) {
                    Toast.makeText(getApplicationContext(), "Please select any item", Toast.LENGTH_LONG).show();
                }

                boolean status = db.isOnWhiteList(ssn);

                PreferenceScreen p2 = (PreferenceScreen) findPreference("add.current.simcard");
                p2.setEnabled(!status);

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_v10_main, menu);
        }
        return true;
    }

    private void loadPrefs() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == ON_CHANGE_SSID) {
            Preference p = findPreference(SSID);
            p.setSummary(serviceHelper.getTetheringSSID());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this, TetheringService.class);
        startService(serviceIntent);
        prefs.edit().putString(SSID, serviceHelper.getTetheringSSID()).apply();
        loadPrefs();
        displayPrompt();
        registerAddSimCardListener();
    }

    private void displayPrompt() {
        if (!prefs.getString(LATEST_VERSION, "").isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)

                .setTitle(R.string.warning)
                .setMessage(getString(R.string.initial_prompt))

                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(ACTIVATE_3G, true).apply();
                        prefs.edit().putBoolean(ACTIVATE_TETHERING, true).apply();
                    }
                })

                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(ACTIVATE_3G, false).apply();
                        prefs.edit().putBoolean(ACTIVATE_TETHERING, false).apply();
                    }
                }).show();
        prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_reset:
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.warning)
                        .setMessage(getString(R.string.reset_prompt))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                prefs.edit().clear().apply();
                            }
                        })
                        .setNegativeButton(R.string.no, null).show();
                return true;
            case R.id.action_exit:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.prompt_onexit)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent serviceIntent = new Intent(MainActivity.this, TetheringService.class);
                                stopService(serviceIntent);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.no, null).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
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
