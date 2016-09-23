package com.labs.dm.auto_tethering.activity;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.view.LayoutInflater;
import android.preference.*;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.LogActivity;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.activity.helpers.*;
import com.labs.dm.auto_tethering.*;
import com.labs.dm.auto_tethering.activity.helpers.RegisterAddSimCardListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterSchedulerListenerHelper;
import com.labs.dm.auto_tethering.db.DBManager;
import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

import java.text.Format;
import java.util.Date;
import java.util.Map;

import static com.labs.dm.auto_tethering.AppProperties.*;

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
    private final int NOTIFICATION_ID = 1234;
    private ListenerManager listenerManager;

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
        adjustSettingForOS();
        onStartup();
        MyLog.clean();
    }

    private void adjustSettingForOS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchOffPreferences("activate.3g", "idle.3g.off", "force.net.from.notify", "usb.internet.force.off", "usb.internet.force.on", "bt.internet.restore.to.initial");
        }
    }

    private void switchOffPreferences(String... names) {
        for (String name : names) {
            findPreference(name).setEnabled(false);
            ((CheckBoxPreference) findPreference(name)).setChecked(false);
        }
    }

    private void registerReceievers() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TetherIntents.EXIT.equals(intent.getAction())) {
                    exitApp();
                } else if (TetherIntents.CLIENTS.equals(intent.getAction())) {
                    final PreferenceScreen connectedClients = (PreferenceScreen) findPreference("idle.connected.clients");
                    connectedClients.setTitle("Connected clients: " + intent.getIntExtra("value", 0));
                } else if (TetherIntents.DATA_USAGE.equals(intent.getAction())) {
                    final PreferenceScreen dataUsage = (PreferenceScreen) findPreference("data.limit.counter");
                    Format dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
                    Format timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                    Date date = new Date(prefs.getLong("data.usage.removeAllData.timestamp", 0));
                    dataUsage.setSummary(String.format("%.2f MB from %s %s", intent.getLongExtra("value", 0) / 1048576f, dateFormat.format(date), timeFormat.format(date)));
                } else if (TetherIntents.UNLOCK.equals(intent.getAction())) {
                    NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nMgr.cancel(NOTIFICATION_ID);
                    PreferenceScreen screen = (PreferenceScreen) findPreference("experimental");
                    int pos = findPreference("data.limit").getOrder();
                    screen.onItemClick(null, null, pos, 0);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(TetherIntents.EXIT);
        filter.addAction(TetherIntents.CLIENTS);
        filter.addAction(TetherIntents.DATA_USAGE);
        filter.addAction(TetherIntents.UNLOCK);
        registerReceiver(receiver, filter);
    }

    private void registerListeners() {
        listenerManager = new ListenerManager(this);
        listenerManager.registerAll();

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
                case "temp.value.stop":
                case "temp.value.start":
                    if ("temp.value.start".equals(p.getKey())) {
                        p.setSummary("When temp. returns to: " + entry.getValue() + " °C");
                    } else if ("temp.value.stop".equals(p.getKey())) {
                        p.setSummary("When temp. higher than: " + entry.getValue() + " °C");
                    }
                    p.getEditor().commit();
                    break;
                case SSID:
                    p.setSummary(serviceHelper.getTetheringSSID());
                    p.getEditor().commit();
                    break;
            }
        }
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
        listenerManager.getHelper(RegisterAddSimCardListenerHelper.class).prepare();
    }

    private void prepareScheduleList() {
        listenerManager.getHelper(RegisterSchedulerListenerHelper.class).prepare();
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_log);
        item.setEnabled(BuildConfig.DEBUG);
        if (!BuildConfig.DEBUG) {
            item.getIcon().setAlpha(128);
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
        prepareSimCardWhiteList();
        prepareScheduleList();
    }

    private void startService() {
        if (!serviceHelper.isServiceRunning(TetheringService.class)) {
            Intent serviceIntent = new Intent(this, TetheringService.class);
            serviceIntent.putExtra("runFromActivity", true);
            startService(serviceIntent);
        }
    }

    private void onStartup() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int version = Integer.parseInt(prefs.getString(LATEST_VERSION, "0"));

                if (version == 0) {
                    /** First start after installation **/
                    prefs.edit().putBoolean(ACTIVATE_3G, false).apply();
                    prefs.edit().putBoolean(ACTIVATE_TETHERING, false).apply();

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.warning)
                            .setMessage(getString(R.string.initial_prompt))
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    prefs.edit().putBoolean(ACTIVATE_3G, true).apply();
                                    prefs.edit().putBoolean(ACTIVATE_TETHERING, true).apply();
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                    prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
                } else if (version < BuildConfig.VERSION_CODE) {
                    /** First start after update **/
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Release notes " + BuildConfig.VERSION_NAME)
                            //.setView(promptsView)
                            .setMessage(getString(R.string.release_notes))
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else if (version == BuildConfig.VERSION_CODE) {
                    /** Another execution **/
                }
            }
        };
        new Handler().post(runnable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log:
                startActivity(new Intent(this, LogActivity.class));
                return true;
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
                                db.removeAllData();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        listenerManager.unregisterAll();
        db.close();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}