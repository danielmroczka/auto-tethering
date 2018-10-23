package com.labs.dm.auto_tethering.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.ListenerManager;
import com.labs.dm.auto_tethering.LogActivity;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.helpers.RegisterAddSimCardListenerHelper;
import com.labs.dm.auto_tethering.activity.helpers.RegisterSchedulerListenerHelper;
import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.db.DBManager;
import com.labs.dm.auto_tethering.receiver.BootCompletedReceiver;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

import java.text.Format;
import java.util.Date;
import java.util.Map;

import de.cketti.library.changelog.ChangeLog;
import io.fabric.sdk.android.Fabric;

import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_KEEP_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_STARTUP;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.LATEST_VERSION;
import static com.labs.dm.auto_tethering.AppProperties.SSID;
import static com.labs.dm.auto_tethering.TetherIntents.CHANGE_CELL_FORM;
import static com.labs.dm.auto_tethering.TetherIntents.SERVICE_ON;
import static com.labs.dm.auto_tethering.TetherIntents.WIFI_DEFAULT_REFRESH;

/**
 * Created by Daniel Mroczka
 */
public class MainActivity extends PermissionsActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int ON_CHANGE_SSID = 1, ON_CHANGE_SCHEDULE = 2;
    private static final int NOTIFICATION_ID = 1234;
    private SharedPreferences prefs;
    private ServiceHelper serviceHelper;
    private BroadcastReceiver receiver;
    private DBManager db;
    private ListenerManager listenerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        db = DBManager.getInstance(getApplicationContext());
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        addPreferencesFromResource(R.xml.preferences);
        serviceHelper = new ServiceHelper(getApplicationContext());
        loadPrefs();
        checkIfNotlocked();
        registerListeners();
        registerReceivers();
        adjustSettingForOS();
        onStartup();
    }

    private void adjustSettingForOS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getActionBar() != null) {
                getActionBar().setDisplayUseLogoEnabled(true);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchOffPreferences("activate.3g", "idle.3g.off", "usb.internet.force.off", "usb.internet.force.on");
        }
    }

    private void switchOffPreferences(String... names) {
        for (String name : names) {
            findPreference(name).setEnabled(false);
            ((CheckBoxPreference) findPreference(name)).setChecked(false);
        }
    }

    private void registerReceivers() {
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
                    Format dateFormat = DateFormat.getDateFormat(getApplicationContext());
                    Format timeFormat = DateFormat.getTimeFormat(getApplicationContext());
                    Date date = new Date(prefs.getLong("data.usage.removeAllData.timestamp", 0));
                    dataUsage.setSummary(String.format("%s from %s %s", Utils.humanReadableByteCount(intent.getLongExtra("value", 0)), dateFormat.format(date), timeFormat.format(date)));
                } else if (TetherIntents.UNLOCK.equals(intent.getAction())) {
                    NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nMgr.cancel(NOTIFICATION_ID);

                    PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("source.activation");
                    final ListAdapter listAdapter = preferenceScreen.getRootAdapter();
                    PreferenceScreen category = (PreferenceScreen) findPreference("data.limit");

                    final int itemsCount = listAdapter.getCount();
                    int itemNumber;
                    for (itemNumber = 0; itemNumber < itemsCount; ++itemNumber) {
                        if (listAdapter.getItem(itemNumber).equals(category)) {
                            preferenceScreen.onItemClick(null, null, itemNumber, 0);
                            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                            context.sendBroadcast(it);
                            Toast.makeText(MainActivity.this, "Please uncheck the property 'Data usage limit on' to unlock!", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                } else if (CHANGE_CELL_FORM.equals(intent.getAction())) {
                    final PreferenceScreen cell = (PreferenceScreen) getPreferenceScreen().findPreference("cell.current");
                    Cellular current = Utils.getCellInfo(MainActivity.this);
                    String styledText = String.format("<small>CID:</small><font color='#00FF40'>%s</font> <small>LAC:</small><font color='#00FF40'>%s</font>", current.getCid(), current.getLac());
                    cell.setTitle("Current Cellular Network:");
                    cell.setSummary(Html.fromHtml(styledText));
                } else if (WIFI_DEFAULT_REFRESH.equals(intent.getAction())) {
                    Preference p = findPreference(SSID);
                    p.setSummary(prefs.getString("default.wifi.network", serviceHelper.getTetheringSSID()));
                    if (serviceHelper.isTetheringWiFi()) {
                        serviceHelper.setWifiTethering(false, null);
                        serviceHelper.setWifiTethering(true, Utils.getDefaultWifiConfiguration(getApplicationContext(), prefs));
                    } else {
                        serviceHelper.setWifiTethering(true, Utils.getDefaultWifiConfiguration(getApplicationContext(), prefs));
                        serviceHelper.setWifiTethering(false, null);
                    }
                    Toast.makeText(getApplicationContext(), "Default WiFi Network has been changed to " + p.getSummary(), Toast.LENGTH_LONG).show();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(TetherIntents.EXIT);
        filter.addAction(TetherIntents.CLIENTS);
        filter.addAction(TetherIntents.DATA_USAGE);
        filter.addAction(TetherIntents.UNLOCK);
        filter.addAction(TetherIntents.WIFI_DEFAULT_REFRESH);
        filter.addAction(CHANGE_CELL_FORM);
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
                case "activate.on.startup.delay":
                    p.setSummary((CharSequence) entry.getValue());
                    p.getEditor().apply();
                    break;
                case "temp.value.stop":
                case "temp.value.start":
                    if ("temp.value.start".equals(p.getKey())) {
                        p.setSummary("When temp. returns to: " + entry.getValue() + " °C");
                    } else if ("temp.value.stop".equals(p.getKey())) {
                        p.setSummary("When temp. higher than: " + entry.getValue() + " °C");
                    }
                    p.getEditor().apply();
                    break;
                case SSID:
                    p.setSummary(serviceHelper.getTetheringSSID());
                    p.getEditor().apply();
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
        if (!BuildConfig.DEBUG) {
            item.setEnabled(false);
            if (item.getIcon() != null) {
                item.getIcon().setAlpha(128);
            }
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
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int version = Integer.parseInt(prefs.getString(LATEST_VERSION, "0"));

                if (version == 0) {
                    /** First start after installation **/
                    prefs.edit().putBoolean(ACTIVATE_3G, false).apply();
                    prefs.edit().putBoolean(ACTIVATE_TETHERING, false).apply();

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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
                    } else {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.warning)
                                .setMessage(getString(R.string.initial_prompt_lollipop))
                                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                    }
                    prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
                }

                if (version < BuildConfig.VERSION_CODE) {
                    /** First start after update **/
                    ChangeLog cl = new ChangeLog(MainActivity.this);
                    if (cl.isFirstRun()) {
                        cl.getLogDialog().show();
                    }
                    prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
                } else if (version == BuildConfig.VERSION_CODE) {
                    /** Another execution **/
                }
            }
        });

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference instanceof PreferenceScreen) {
            initializeActionBar((PreferenceScreen) preference);
        }

        return false;
    }

    private void initializeActionBar(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        if (dialog != null) {
            View homeBtn = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && dialog.getActionBar() != null) {
                dialog.getActionBar().setDisplayHomeAsUpEnabled(true);
                homeBtn = dialog.findViewById(android.R.id.home);
            }

            if (homeBtn != null) {
                View.OnClickListener dismissDialogClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                };

                ViewParent homeBtnContainer = homeBtn.getParent();

                if (homeBtnContainer instanceof FrameLayout) {
                    ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

                    if (containerParent instanceof LinearLayout) {
                        containerParent.setOnClickListener(dismissDialogClickListener);
                    } else {
                        ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
                    }
                } else {
                    homeBtn.setOnClickListener(dismissDialogClickListener);
                }
            }
        }
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
                                prefs.edit().putString(LATEST_VERSION, String.valueOf(BuildConfig.VERSION_CODE)).apply();
                                db.removeAllData();
                                prepareSimCardWhiteList();
                                prepareScheduleList();
                                restartApp();
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

    private void restartApp() {
        Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), 123456, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, mPendingIntent);
        finish();
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
                sendBroadcast(new Intent(SERVICE_ON));
                break;
            case ACTIVATE_TETHERING:
                sendBroadcast(new Intent(SERVICE_ON));
                break;
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
        //db.close(); TODO Check if it's OK
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