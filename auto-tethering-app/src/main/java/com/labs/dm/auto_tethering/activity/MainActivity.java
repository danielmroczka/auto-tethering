package com.labs.dm.auto_tethering.activity;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.DBManager;
import com.labs.dm.auto_tethering.db.SimCard;
import com.labs.dm.auto_tethering.receiver.BluetoothBroadcastReceiver;
import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;
import com.labs.dm.auto_tethering.ui.SchedulePreference;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public static final int ON_CHANGE_SSID = 1;
    public static final int ON_CHANGE_SCHEDULE = 2;
    private SharedPreferences prefs;
    private ServiceHelper serviceHelper;
    private BroadcastReceiver receiver;
    private DBManager db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        db = DBManager.getInstance(getApplicationContext());
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        serviceHelper = new ServiceHelper(getApplicationContext());
        loadPrefs();

        checkIfNotlocked();
        registerListeners();
        registerReceievers();
    }

    private void registerReceievers() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("exit".equals(intent.getAction())) {
                    exitApp();
                } else if ("clients".equals(intent.getAction())) {
                    final PreferenceScreen connectedClients = (PreferenceScreen) findPreference("idle.connected.clients");
                    connectedClients.setTitle("Connected clients: " + intent.getIntExtra("value", 0));
                } else if ("data.usage".equals(intent.getAction())) {
                    final PreferenceScreen dataUsage = (PreferenceScreen) findPreference("data.limit.counter");
                    dataUsage.setSummary(String.format("%.2f MB", intent.getLongExtra("value", 0) / 1048576f));
                } else if ("unlock".equals(intent.getAction())) {
                    NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nMgr.cancel(1234);
                    PreferenceScreen screen = (PreferenceScreen) findPreference("experimental");
                    int pos = findPreference("data.limit").getOrder();
                    screen.onItemClick(null, null, pos, 0);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("exit");
        filter.addAction("clients");
        filter.addAction("data.usage");
        filter.addAction("unlock");
        registerReceiver(receiver, filter);
    }

    private void registerListeners() {
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

        final CheckBoxPreference activationStartup = (CheckBoxPreference) findPreference("activate.on.startup");
        final ComponentName componentName = new ComponentName(MainActivity.this, BootCompletedReceiver.class);
        int state = getPackageManager().getComponentEnabledSetting(componentName);

        activationStartup.setChecked(state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

        activationStartup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                int state = getPackageManager().getComponentEnabledSetting(componentName);

                if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                    getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    Toast.makeText(getApplicationContext(), R.string.on_startup_enable, Toast.LENGTH_LONG).show();
                } else {
                    getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    Toast.makeText(getApplicationContext(), R.string.on_startup_disable, Toast.LENGTH_LONG).show();
                }

                return true;
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

        EditTextPreference batteryLevelValue = (EditTextPreference) findPreference("usb.off.battery.lvl.value");
        batteryLevelValue.setOnPreferenceChangeListener(changeListener);
        batteryLevelValue.getEditText().setFilters(new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        int input = Integer.parseInt(dest.toString() + source.toString());
                        if (0 < input && input <= 100) {
                            return null;
                        }
                        return "";
                    }
                }});

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Preference p = findPreference(entry.getKey());

            switch (entry.getKey()) {
                case IDLE_3G_OFF_TIME:
                case IDLE_TETHERING_OFF_TIME:
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

        Preference p = findPreference(SSID);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(preference.getIntent(), ON_CHANGE_SSID);
                return true;
            }
        });

        PreferenceScreen usbTethering = (PreferenceScreen) findPreference("usb.tethering");
        usbTethering.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                serviceHelper.usbTethering(true);
                return false;
            }
        });

        PreferenceScreen resetDataUsage = (PreferenceScreen) findPreference("data.limit.reset");
        resetDataUsage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(MainActivity.this)
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

                                Intent intent = new Intent("data.usage");
                                sendBroadcast(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null
                        ).show();

                return true;
            }
        });

        EditTextPreference dataLimit = (EditTextPreference) findPreference("data.limit.value");
        dataLimit.setOnPreferenceChangeListener(changeListener);

        final CheckBoxPreference btCheckBox = (CheckBoxPreference) findPreference("bt.start.discovery");
        final ComponentName componentName2 = new ComponentName(MainActivity.this, BluetoothBroadcastReceiver.class);
        btCheckBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (btCheckBox.isChecked()) {
                    if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        BluetoothAdapter.getDefaultAdapter().enable();
                    }
                    getPackageManager().setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    sendBroadcast(new Intent("bt.set.idle"));
                    getPackageManager().setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }

//                int state = getPackageManager().getComponentEnabledSetting(componentName2);
//
//
//                if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
//                    if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
//                        BluetoothAdapter.getDefaultAdapter().enable();
//                    }
//                    getPackageManager().setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
//                } else {
//                    sendBroadcast(new Intent("bt.set.idle"));
//                    getPackageManager().setComponentEnabledSetting(componentName2, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
//                }

                return true;
            }
        });
        btCheckBox.setChecked(prefs.getBoolean("bt.start.discovery", false));
    }

    /**
     * Method checks if service is locked to startup on system boot.
     * If founds that service is blocked Dialog will be displayed with choices:
     * - to unblock (Yes)
     * - cancel (No)
     * - switch of next invocation of this Dialog (Don't Remind)
     */
    private void checkIfNotlocked() {
        final ComponentName componentName = new ComponentName(this, BootCompletedReceiver.class);
        int state = getPackageManager().getComponentEnabledSetting(componentName);

        if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && !prefs.getBoolean("autostart.blocked.donotremind", false)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.warning)
                    .setMessage("Startup application on system boot is currently blocked and therefore service cannot run properly.\n\nDo you want to enable this setting?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final CheckBoxPreference activationStartup = (CheckBoxPreference) findPreference("activate.on.startup");
                            activationStartup.setChecked(true);
                            getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                            Toast.makeText(getApplicationContext(), R.string.on_startup_enable, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNeutralButton(R.string.donot_remind, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().putBoolean("autostart.blocked.donotremind", true).apply();
                        }
                    })
                    .setNegativeButton(R.string.no, null
                    ).show();
        }
    }

    private void prepareSimCardWhiteList() {
        PreferenceCategory pc = (PreferenceCategory) findPreference("simcard.list");
        List<SimCard> list = db.readSimCard();
        for (int idx = 0; idx < pc.getPreferenceCount(); idx++) {
            Object object = pc.getPreference(idx);
            if (object instanceof CheckBoxPreference) {
                pc.removePreference((CheckBoxPreference) object);
            }
        }
        for (SimCard item : list) {
            Preference ps = new CheckBoxPreference(getApplicationContext());
            ps.setTitle(item.getNumber());
            ps.setSummary("SSN: " + item.getSsn());
            pc.addPreference(ps);
        }

        PreferenceScreen ps = (PreferenceScreen) findPreference("add.current.simcard");
        ps.setEnabled(true);
    }

    private void prepareScheduleList() {
        final PreferenceCategory p = (PreferenceCategory) findPreference("scheduled.shutdown.list");
        List<Cron> list = db.getCrons();

        p.removeAll();
        for (final Cron cron : list) {
            final SchedulePreference ps = new SchedulePreference(p, cron, this);
            String title;
            if (cron.getHourOff() == -1) {
                title = String.format(Locale.ENGLISH, "ON at %02d:%02d", cron.getHourOn(), cron.getMinOn());
            } else if (cron.getHourOn() == -1) {
                title = String.format(Locale.ENGLISH, "OFF at %02d:%02d", cron.getHourOff(), cron.getMinOff());
            } else {
                title = String.format(Locale.ENGLISH, "%02d:%02d - %02d:%02d", cron.getHourOff(), cron.getMinOff(), cron.getHourOn(), cron.getMinOn());
            }
            ps.setTitle(title);
            ps.setSummary(Utils.maskToDays(cron.getMask()));
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
                PreferenceCategory cat = (PreferenceCategory) findPreference("scheduled.shutdown.list");
                if (cat.getPreferenceCount() >= 10) {
                    Toast.makeText(getApplicationContext(), "You cannot add more than 10 schedule items!", Toast.LENGTH_LONG).show();
                    return false;
                }
                startActivityForResult(new Intent(MainActivity.this, ScheduleActivity.class), ON_CHANGE_SCHEDULE);
                return true;
            }
        });
    }

    private void registerBTListener() {
        PreferenceScreen p = (PreferenceScreen) findPreference("bt.add.device");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final PreferenceCategory category = (PreferenceCategory) findPreference("bt.list");

                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                builderSingle.setIcon(R.drawable.app);
                builderSingle.setTitle("Select One Item");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.select_dialog_singlechoice);

                Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    arrayAdapter.add(device.getName());
                }

                builderSingle.setNegativeButton(
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which >= 0) {
                            String name = arrayAdapter.getItem(which);

                            boolean found = false;
                            Map<String, ?> map = prefs.getAll();
                            for (Map.Entry<String, ?> entry : map.entrySet()) {
                                if (entry.getKey().startsWith("bt.devices.") && entry.getValue().equals(name)) {
                                    found = true;
                                }
                            }
                            if (!found) {
                                prefs.edit().putString("bt.devices." + name, name).apply();
                                Preference ps = new CheckBoxPreference(getApplicationContext());
                                ps.setTitle(name);
                                category.addPreference(ps);
                            } else {
                                Toast.makeText(getApplicationContext(), "Device already added!", Toast.LENGTH_LONG).show();
                            }
                        }
                        dialog.dismiss();
                    }
                });
                builderSingle.show();
                return false;
            }
        });

        PreferenceScreen p2 = (PreferenceScreen) findPreference("bt.remove.device");
        p2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                PreferenceCategory p = (PreferenceCategory) findPreference("bt.list");
                boolean changed = false;
                for (int idx = 0; idx < p.getPreferenceCount(); idx++) {
                    Preference pref = p.getPreference(idx);
                    if (pref instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) pref).isChecked();
                        if (status) {
                            prefs.edit().remove("bt.devices." + pref.getTitle()).apply();
                            p.removePreference(pref);
                            changed = true;
                        }
                    }
                }

                if (!changed) {
                    Toast.makeText(getApplicationContext(), "Please select any item", Toast.LENGTH_LONG).show();
                }
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
        registerAddSimCardListener();
        registerAddSchedule();
        registerBTListener();
        prepareSimCardWhiteList();
        prepareBTList();
        prepareScheduleList();
    }

    private void prepareBTList() {
        PreferenceCategory pc = (PreferenceCategory) findPreference("bt.list");

        Map<String, ?> map = prefs.getAll();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Log.i("MAIN", entry.getKey());
            if (entry.getKey().startsWith("bt.devices.")) {
                Preference ps = new CheckBoxPreference(getApplicationContext());
                ps.setTitle(String.valueOf(entry.getValue()));
                if (ps.getTitle() != null) {
                    Log.i("MAIN2", String.valueOf(ps.getTitle()));
                    pc.addPreference(ps);
                }
            }
        }
    }

    private void startService() {
        if (!serviceHelper.isServiceRunning(TetheringService.class)) {
            Intent serviceIntent = new Intent(this, TetheringService.class);
            serviceIntent.putExtra("runFromActivity", true);
            startService(serviceIntent);
        }
    }

    private void displayPromptAtStartup() {
        int version = Integer.parseInt(prefs.getString(LATEST_VERSION, "0"));

        if (version == 0) {
            /** First start after installation **/
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
                    })
                    .show();
            prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
        } else if (version < BuildConfig.VERSION_CODE) {

            /** First start after update **/
            new AlertDialog.Builder(this)
                    .setTitle("Release notes " + BuildConfig.VERSION_NAME)
                    .setMessage(getString(R.string.release_notes))
                    .setPositiveButton("Close", null)
                    .show();
            prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
        } else if (version == BuildConfig.VERSION_CODE) {
            /** Another execution **/
        }
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
                                prepareSimCardWhiteList();
                                prepareScheduleList();
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
        Intent serviceIntent = new Intent(this, TetheringService.class);
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

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        displayPromptAtStartup();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }
}
