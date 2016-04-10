package com.labs.dm.auto_tethering.activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.*;
import android.widget.*;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.ScheduleCheckBoxPreference;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.DBManager;
import com.labs.dm.auto_tethering.db.SimCard;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_KEEP_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_STARTUP;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.LATEST_VERSION;
import static com.labs.dm.auto_tethering.AppProperties.RETURN_TO_PREV_STATE;
import static com.labs.dm.auto_tethering.AppProperties.SSID;

/**
 * Created by Daniel Mroczka
 */
public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int ON_CHANGE_SSID = 1;
    private static final int ON_CHANGE_SCHEDULE = 2;
    private SharedPreferences prefs;
    private ServiceHelper serviceHelper;
    private DBManager db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DBManager.getInstance(getApplicationContext());
        addPreferencesFromResource(R.xml.preferences);
        serviceHelper = new ServiceHelper(getApplicationContext());
        loadPrefs();

        Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        };

        Preference.OnPreferenceChangeListener revertStateCheckBoxListener = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {

                    Toast toast = Toast.makeText(getApplicationContext(), "Once application has been closed tethering and internet connection state will be restored to state before open this application", Toast.LENGTH_LONG);
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

        PreferenceScreen editSSID = (PreferenceScreen) findPreference(SSID);
        editSSID.setOnPreferenceChangeListener(changeListener);

        EditTextPreference tetheringIdleTime = (EditTextPreference) findPreference(IDLE_TETHERING_OFF_TIME);
        tetheringIdleTime.setOnPreferenceChangeListener(changeListener);
        EditTextPreference internetIdleTime = (EditTextPreference) findPreference(IDLE_3G_OFF_TIME);
        internetIdleTime.setOnPreferenceChangeListener(changeListener);

        CheckBoxPreference revertStateCheckBox = (CheckBoxPreference) findPreference(RETURN_TO_PREV_STATE);
        revertStateCheckBox.setOnPreferenceChangeListener(revertStateCheckBoxListener);

        final PreferenceScreen connectedClients = (PreferenceScreen) findPreference("idle.connected.clients");

        connectedClients.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                connectedClients.setTitle("Connected clients: " + Utils.connectedClients());
                return false;
            }
        });

        CheckBoxPreference keepServiceCheckBox = (CheckBoxPreference) findPreference(ACTIVATE_KEEP_SERVICE);
        keepServiceCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    startService();
                }
                return true;
            }
        });

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Preference p = findPreference(entry.getKey());

            switch (entry.getKey()) {
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
        prepareScheduleList();
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
            ps.setSummary("SSN: " + item.getSsn());
            p.addPreference(ps);
        }
    }

    private void prepareScheduleList() {
        final PreferenceCategory p = (PreferenceCategory) findPreference("scheduled.shutdown.list");
        List<Cron> list = db.getCron();
        Collections.sort(list, new Comparator<Cron>() {
            @Override
            public int compare(Cron lhs, Cron rhs) {
                int diffOff = 60 * (lhs.getHourOff() - rhs.getHourOff()) + (lhs.getMinOff() - rhs.getMinOff());
                int diffOn = 60 * (lhs.getHourOn() - rhs.getHourOn()) + (lhs.getMinOn() - rhs.getMinOn());
                return diffOff > 0 ? diffOff : diffOn;
            }
        });
        p.removeAll();
        for (final Cron cron : list) {
            final ScheduleCheckBoxPreference ps = new ScheduleCheckBoxPreference(p, cron, getApplicationContext());
            String title = String.format("%02d:%02d - %02d:%02d", cron.getHourOff(), cron.getMinOff(), +cron.getHourOn(), cron.getMinOn());
            ps.setTitle(title);
            ps.setSummary(Utils.maskToDays(cron.getMask()));
            ps.setId(cron.getId());
            p.addPreference(ps);

        }
    }

    private void addSimCard(String number) {
        final TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        final String ssn = tMgr.getSimSerialNumber();
        SimCard simcard = new SimCard(tMgr.getSimSerialNumber(), number, 0);
        db.addSimCard(simcard);
        boolean status = db.isOnWhiteList(ssn);
        PreferenceScreen p = (PreferenceScreen) findPreference("add.current.simcard");
        p.setEnabled(!status);
        prepareSimCardWhiteList();
    }

    private void registerAddSchedule() {
        PreferenceScreen p = (PreferenceScreen) findPreference("scheduler.add");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(MainActivity.this, ScheduleActivity.class), ON_CHANGE_SCHEDULE);
                return true;
            }
        });
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
                            .setMessage("Cannot retrieve telephone number. Please provide it manually")
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
                            String ssn = ((CheckBoxPreference) object).getSummary().toString();
                            if (ssn != null) {
                                ssn = ssn.substring("SSN: ".length()).trim();
                                db.removeSimCard(ssn);
                                p.removePreference((Preference) object);
                                changed = true;
                            }
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
            if (resCode == android.app.Activity.RESULT_OK) {
                Preference p = findPreference(SSID);
                p.setSummary(serviceHelper.getTetheringSSID());
            }
        }
        if (reqCode == ON_CHANGE_SCHEDULE) {
            if (resCode == android.app.Activity.RESULT_OK) {
                prepareScheduleList();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        startService();
        prefs.edit().putString(SSID, serviceHelper.getTetheringSSID()).apply();
        loadPrefs();
        displayPrompt();
        registerAddSimCardListener();
        registerAddSchedule();
    }

    private void startService() {
        if (!isServiceRunning(TetheringService.class)) {
            Intent serviceIntent = new Intent(this, TetheringService.class);
            serviceIntent.putExtra("runFromActivity", true);
            startService(serviceIntent);
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void displayPrompt() {
        if (!prefs.getString(LATEST_VERSION, "").isEmpty()) {
            return;
        }

        prefs.edit().putBoolean(ACTIVATE_3G, false).apply();
        prefs.edit().putBoolean(ACTIVATE_TETHERING, false).apply();

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
                                db.reset();
                            }
                        })
                        .setNegativeButton(R.string.no, null).show();
                return true;
            case R.id.action_exit:
                if (prefs.getBoolean(ACTIVATE_KEEP_SERVICE, true)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.prompt_onexit)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    exitApp();
                                }
                            })
                            .setNegativeButton(R.string.no, null).show();
                } else {
                    exitApp();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void exitApp() {
        Intent serviceIntent = new Intent(MainActivity.this, TetheringService.class);
        stopService(serviceIntent);
        finish();
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
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }
}
