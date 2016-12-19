package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Cellular;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.Cron.STATUS;
import com.labs.dm.auto_tethering.db.DBManager;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_KEEP_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_ROAMING;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_ROAMING_HC;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_SIMCARD;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.DEFAULT_IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.RETURN_TO_PREV_STATE;
import static com.labs.dm.auto_tethering.TetherIntents.BT_CONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_DISCONNECTED;
import static com.labs.dm.auto_tethering.TetherIntents.BT_RESTORE;
import static com.labs.dm.auto_tethering.TetherIntents.BT_SEARCH;
import static com.labs.dm.auto_tethering.TetherIntents.CHANGE_NETWORK_STATE;
import static com.labs.dm.auto_tethering.TetherIntents.EVENT_MOBILE_OFF;
import static com.labs.dm.auto_tethering.TetherIntents.EVENT_MOBILE_ON;
import static com.labs.dm.auto_tethering.TetherIntents.EVENT_TETHER_OFF;
import static com.labs.dm.auto_tethering.TetherIntents.EVENT_TETHER_ON;
import static com.labs.dm.auto_tethering.TetherIntents.EVENT_WIFI_OFF;
import static com.labs.dm.auto_tethering.TetherIntents.EVENT_WIFI_ON;
import static com.labs.dm.auto_tethering.TetherIntents.EXIT;
import static com.labs.dm.auto_tethering.TetherIntents.RESUME;
import static com.labs.dm.auto_tethering.TetherIntents.TEMPERATURE_ABOVE_LIMIT;
import static com.labs.dm.auto_tethering.TetherIntents.TEMPERATURE_BELOW_LIMIT;
import static com.labs.dm.auto_tethering.TetherIntents.TETHERING;
import static com.labs.dm.auto_tethering.TetherIntents.USB_OFF;
import static com.labs.dm.auto_tethering.TetherIntents.USB_ON;
import static com.labs.dm.auto_tethering.TetherIntents.WIDGET;
import static com.labs.dm.auto_tethering.Utils.adapterDayOfWeek;
import static com.labs.dm.auto_tethering.service.ServiceAction.BLUETOOTH_INTERNET_TETHERING_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.BLUETOOTH_INTERNET_TETHERING_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.CELL_INTERNET_TETHERING_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.CELL_INTERNET_TETHERING_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.DATA_USAGE_EXCEED_LIMIT;
import static com.labs.dm.auto_tethering.service.ServiceAction.INTERNET_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.INTERNET_OFF_IDLE;
import static com.labs.dm.auto_tethering.service.ServiceAction.INTERNET_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.ROAMING_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_INTERNET_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_INTERNET_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_TETHER_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_TETHER_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.SIMCARD_BLOCK;
import static com.labs.dm.auto_tethering.service.ServiceAction.TEMP_TETHERING_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.TEMP_TETHERING_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.TETHER_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.TETHER_OFF_IDLE;
import static com.labs.dm.auto_tethering.service.ServiceAction.TETHER_ON;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private static final String TAG = "TetheringService";
    private static final int CHECK_DELAY = 5;
    private static final int NOTIFICATION_ID = 1234;
    private static final int MINUTE_IN_MS = 60000;

    private boolean forceOff = false, forceOn = false;
    private boolean changeMobileState;
    private boolean initial3GStatus, initialTetheredStatus, initialBluetoothStatus, initialWifiStatus, wifiWasEnabled;
    private boolean tetheringProcessing;
    private boolean blockForceInternet;
    private boolean runFromActivity;
    private boolean flag = true;
    private boolean internetOn;
    private long lastAccess = getTime().getTimeInMillis();
    private BroadcastReceiver receiver;
    private String lastNotificationTickerText;
    private String connectedDeviceName;
    private List<Cron> crons;
    private SharedPreferences prefs;
    private ServiceHelper serviceHelper;
    private Notification notification;
    private Timer timer;
    private TimerTask dataUsageTask, bluetoothTask;

    private enum ScheduleResult {
        ON, OFF, NONE
    }

    private enum Status {
        DEFAULT,
        DEACTIVATED_ON_IDLE,
        ACTIVATED_ON_SCHEDULE,
        DEACTIVATED_ON_SCHEDULE,
        USB_ON,
        DATA_USAGE_LIMIT_EXCEED,
        ACTIVATED_ON_CELL, DEACTIVATED_ON_CELL,
        TEMPERATURE_OFF,
        BT
    }

    private Status previousStatus, status = Status.DEFAULT;

    private final String[] invents = {TETHERING, WIDGET, RESUME, EXIT, USB_ON, USB_OFF,
            BT_RESTORE, BT_CONNECTED, BT_DISCONNECTED, BT_SEARCH, TEMPERATURE_ABOVE_LIMIT, TEMPERATURE_BELOW_LIMIT, CHANGE_NETWORK_STATE, TetherIntents.TETHER_ON, TetherIntents.TETHER_OFF, TetherIntents.INTERNET_ON, TetherIntents.INTERNET_OFF,
            EVENT_TETHER_OFF, EVENT_TETHER_ON, EVENT_MOBILE_OFF, EVENT_MOBILE_ON, EVENT_WIFI_OFF, EVENT_WIFI_ON
    };

    public TetheringService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceHelper = new ServiceHelper(getApplicationContext());
        onChangeProperties();
        init();
        registerReceivers();
        registerTimeTask();
    }

    private void registerTimeTask() {
        dataUsageTask = new DataUsageTimerTask(getApplicationContext(), prefs);
        bluetoothTask = new BluetoothTimerTask(getApplicationContext(), prefs);
        timer = new Timer();
        timer.schedule(dataUsageTask, 1000, 15000);
        timer.schedule(bluetoothTask, 5000, 30000);
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        for (String invent : invents) {
            filter.addAction(invent);
        }
        receiver = new MyBroadcastReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        MyLog.clean();
        runAsForeground();
        if (prefs.getBoolean("data.limit.startup.reset", false)) {
            MyLog.i("Datausage", "Reset data usage at startup");
            Utils.resetDataUsageStat(prefs, -ServiceHelper.getDataUsage(), 0);
            Intent onIntent = new Intent(TetherIntents.DATA_USAGE);
            onIntent.putExtra("value", 0);
        }
    }

    private void init() {
        initial3GStatus = serviceHelper.isConnectedToInternetThroughMobile();
        initialTetheredStatus = serviceHelper.isTetheringWiFi();
        initialBluetoothStatus = serviceHelper.isBluetoothActive();
        initialWifiStatus = serviceHelper.isConnectedToInternetThroughWiFi();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyLog.i(TAG, "onStartCommand");
        runFromActivity = intent.getBooleanExtra("runFromActivity", false);
        int state = intent.getIntExtra("state", -1);
        if (state == 1) {
            execute(TETHER_OFF);
        } else if (state == 0) {
            execute(TETHER_ON);
        }
        if (intent.getBooleanExtra("usb.on", false)) {
            sendBroadcast(new Intent(USB_ON));
        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (isServiceActivated()) {
            showNotification(getString(R.string.service_started), getNotificationIcon());

            if (!isCorrectSimCard()) {
                execute(SIMCARD_BLOCK);
            }

            if (!allowRoaming()) {
                showNotification(getString(R.string.roaming_service_disabled), R.drawable.app_off);
            }

            onService();
        }

        while (flag) {
            try {
                boolean connected3G = serviceHelper.isConnectedToInternetThroughMobile();
                boolean tethered = serviceHelper.isTetheringWiFi();

                if (!(forceOff || forceOn) && (isServiceActivated() || keepService())) {
                    if (enabled()) {
                        checkCellular();
                        boolean idle = checkIdle();
                        ScheduleResult res = scheduler();
                        if (res == ScheduleResult.OFF) {
                            execute(SCHEDULED_INTERNET_OFF, SCHEDULED_TETHER_OFF);
                        } else if (res == ScheduleResult.ON) {
                            setStatus(Status.DEFAULT);
                            if (isActivated3G()) {
                                execute(SCHEDULED_INTERNET_ON);
                            }
                            execute(SCHEDULED_TETHER_ON);
                        } else if (idle && serviceHelper.isTetheringWiFi()) {
                            if (check3GIdle()) {
                                execute(INTERNET_OFF_IDLE);
                            }
                            if (checkWifiIdle()) {
                                execute(TETHER_OFF_IDLE);
                            }
                        } else if (status == Status.DEFAULT) {
                            onService();
                        }

                    } else {

                        if (tethered || connected3G) {
                            if (!isCorrectSimCard()) {
                                execute(SIMCARD_BLOCK, R.string.simcard_service_disabled);
                            } else if (!allowRoaming()) {
                                execute(ROAMING_OFF, R.string.roaming_service_disabled);
                            }
                        }
                    }
                } else if (forceOn) {
                    if (!blockForceInternet && !serviceHelper.isConnectedToInternetThroughMobile()) {
                        execute(INTERNET_ON);
                    }
                    if (!serviceHelper.isTetheringWiFi()) {
                        execute(TETHER_ON);
                    }
                }
                if (!keepService()) {
                    flag = false;
                }

                long usage = ServiceHelper.getDataUsage() + prefs.getLong("data.usage.removeAllData.value", 0);

                if (prefs.getBoolean("data.limit.on", false)) {
                    if (usage / (1048576f) > Integer.parseInt(prefs.getString("data.limit.value", "0"))) {
                        execute(DATA_USAGE_EXCEED_LIMIT);
                    }
                } else {
                    if (status == Status.DATA_USAGE_LIMIT_EXCEED) {
                        setStatus(Status.DEFAULT);
                    }
                }

                TimeUnit.SECONDS.sleep(CHECK_DELAY);
            } catch (InterruptedException e) {
                MyLog.e(TAG, e.getMessage());
            }
        }
    }

    private boolean onActivationList(String type, Cellular current) {
        List<Cellular> actives = DBManager.getInstance(this).readCellular(type);
        return actives.indexOf(current) >= 0;
    }

    private void checkCellular() {
        Cellular current = Utils.getCellInfo(getApplicationContext());

        boolean isOnActivationList = onActivationList("A", current);
        boolean isOnDeactivationList = onActivationList("D", current);

        if (!serviceHelper.isTetheringWiFi() && isOnActivationList && usbConnection()) {
            execute(CELL_INTERNET_TETHERING_ON);
        } else if (serviceHelper.isTetheringWiFi() && isOnDeactivationList) {
            execute(CELL_INTERNET_TETHERING_OFF);
        } else if (!isOnActivationList && (status == Status.ACTIVATED_ON_CELL || status == Status.DEACTIVATED_ON_CELL)) {
            execute(CELL_INTERNET_TETHERING_OFF);
        }
    }

    private boolean enabled() {
        return isCorrectSimCard() && allowRoaming();
    }

    private boolean batteryLevel() {
        boolean batteryLevel = prefs.getBoolean("usb.off.battery.lvl", false);
        return !batteryLevel || 100f * serviceHelper.batteryLevel() >= Integer.valueOf(prefs.getString("usb.off.battery.lvl.value", "15"));
    }

    /**
     * Returns true if properties is set 'usbConnection.only.when.connected' and device is pluged to usbConnection or ac charger
     *
     * @return
     */
    private boolean usbConnection() {
        boolean flag = prefs.getBoolean("usb.only.when.connected", false);
        return (!flag || serviceHelper.isPluggedToPower()) && batteryLevel();
    }

    private boolean keepService() {
        return prefs.getBoolean(ACTIVATE_KEEP_SERVICE, true);
    }

    /**
     * Turns tethering in separate thread.
     *
     * @param state
     * @return true if changed the state or false if not
     */
    private boolean tetheringAsyncTask(boolean state) {
        if (serviceHelper.isTetheringWiFi() == state) {
            return false;
        }

        if (Utils.isAirplaneModeOn(getApplicationContext())) {
            showNotification("Tethering blocked due to activated Airplane Mode", getNotificationIcon());
            return false;
        }

        if (state && prefs.getBoolean("wifi.connected.block.tethering", false) && serviceHelper.isConnectedToInternetThroughWiFi()) {
            connectedDeviceName = null;
            showNotification("Tethering blocked due to active connection to WiFi Network", getNotificationIcon());
            return false;
        }

        if (tetheringProcessing) {
            return false;
        }

        tetheringProcessing = true;
        if (state && !serviceHelper.isTetheringWiFi()) {
            wifiWasEnabled = serviceHelper.isWifiEnabled();//serviceHelper.isConnectedToInternetThroughWiFi();
        }

        new TurnOnTetheringAsyncTask().doInBackground(state);
        return true;
    }

    private boolean isServiceActivated() {
        return runFromActivity || prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false);
    }

    /**
     * Iterates through all cron items.
     *
     * @return ScheduleResult
     */
    private ScheduleResult scheduler() {
        Calendar now = getTime();
        onChangeProperties();
        boolean state = false, changed = false;
        for (Cron cron : crons) {
            boolean matchedMask = (cron.getMask() & (int) Math.pow(2, adapterDayOfWeek(now.get(Calendar.DAY_OF_WEEK)))) > 0;
            boolean active = cron.getStatus() == STATUS.SCHED_OFF_ENABLED.getValue();
            if (!active || !matchedMask) {
                continue;
            }

            Calendar timeOff = getTime();
            adjustCalendar(timeOff, cron.getHourOff(), cron.getMinOff());
            Calendar timeOn = getTime();
            adjustCalendar(timeOn, cron.getHourOn(), cron.getMinOn());

            if (cron.getHourOff() == -1) {
                long diff = now.getTimeInMillis() - timeOn.getTimeInMillis();
                if (diff > 0 && CHECK_DELAY * 1000 >= diff) {
                    return ScheduleResult.ON;
                }
            } else if (cron.getHourOn() == -1) {
                long diff = now.getTimeInMillis() - timeOff.getTimeInMillis();
                if (diff > 0 && CHECK_DELAY * 1000 >= diff) {
                    return ScheduleResult.OFF;
                }
            } else {
                long diff = now.getTimeInMillis() - timeOn.getTimeInMillis();
                if (diff > 0 && CHECK_DELAY * 1000 >= diff) {
                    changed = true;
                }
                boolean scheduled = timeOff.getTimeInMillis() < now.getTimeInMillis() && now.getTimeInMillis() < timeOn.getTimeInMillis();
                state |= scheduled;
            }
        }

        return state ? ScheduleResult.OFF : changed ? ScheduleResult.ON : ScheduleResult.NONE;
    }

    private void adjustCalendar(Calendar calendar, int hour, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
    }

    private Calendar getTime() {
        return Calendar.getInstance();
    }

    /**
     * Returns true when idle settings are switched on and no client is connected
     *
     * @return
     */
    private boolean checkIdle() {
        Intent onIntent = new Intent(TetherIntents.CLIENTS);
        onIntent.putExtra("value", Utils.connectedClients());
        sendBroadcast(onIntent);
        if (prefs.getBoolean(IDLE_3G_OFF, false) || prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (Utils.connectedClients() > 0) {
                updateLastAccess();
                return false;
            }

            return true;
        } else {
            updateLastAccess();
            if (status == Status.DEACTIVATED_ON_IDLE) {
                setStatus(Status.DEFAULT);
            }
        }
        return false;
    }

    private void updateLastAccess() {
        lastAccess = getTime().getTimeInMillis();
    }

    private boolean check3GIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false)) {
            if (getTime().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_3G_OFF_TIME, "60")) * MINUTE_IN_MS) {
                return true;
            }
        }

        return false;
    }

    private boolean checkWifiIdle() {
        if (prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (getTime().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_TETHERING_OFF_TIME, DEFAULT_IDLE_TETHERING_OFF_TIME)) * MINUTE_IN_MS) {
                return true;
            }
        }
        return false;
    }

    private void onChangeProperties() {
        crons = DBManager.getInstance(getApplicationContext()).getCrons();
    }

    /**
     * Turns mobile data in separate thread.
     *
     * @param state
     * @return true if changed the state or false if not
     */
    private boolean internetAsyncTask(boolean state) {
        if (serviceHelper.isConnectedToInternetThroughMobile() == state) {
            return false;
        }

        if (state && Utils.isAirplaneModeOn(getApplicationContext())) {
            return false;
        }

        new TurnOn3GAsyncTask().doInBackground(state);
        return true;
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(ACTIVATE_TETHERING, false) && usbConnection();
    }

    private boolean isActivated3G() {
        return prefs.getBoolean(ACTIVATE_3G, false) && usbConnection();
    }

    private boolean allowRoaming() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isRoaming = manager.isNetworkRoaming();
        boolean withinHomeCountry = manager.getNetworkCountryIso() != null && manager.getNetworkCountryIso().equals(manager.getSimCountryIso());
        boolean allowRoaming = prefs.getBoolean(ACTIVATE_ON_ROAMING, false);
        boolean allowRoamingHomeCountry = prefs.getBoolean(ACTIVATE_ON_ROAMING_HC, false);
        return (!isRoaming || (withinHomeCountry && allowRoamingHomeCountry) || allowRoaming);
    }

    private boolean isCorrectSimCard() {
        if (prefs.getBoolean(ACTIVATE_ON_SIMCARD, false)) {
            TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String simSerialNumber = tMgr.getSimSerialNumber();
            return simSerialNumber != null && DBManager.getInstance(getApplicationContext()).isOnWhiteList(simSerialNumber);
        } else {
            return true;
        }
    }

    private class TurnOn3GAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            internetOn = params[0];
            serviceHelper.setMobileDataEnabled(params[0]);
            return null;
        }
    }

    private class TurnOnBTAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            internetOn = params[0];
            serviceHelper.setBluetoothStatus(params[0]);
            return null;
        }
    }

    private class TurnOnTetheringAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            updateLastAccess();
            serviceHelper.setWifiTethering(params[0]);
            return null;
        }
    }

    private void runAsForeground() {
        if (notification == null) {
            this.notification = buildNotification(getString(R.string.service_started));
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String caption) {
        return buildNotification(caption, getNotificationIcon());
    }

    private int getNotificationIcon() {
        boolean tethering = serviceHelper.isTetheringWiFi();
        boolean internet = serviceHelper.isConnectedToInternetThroughMobile();

        if (tethering && internet) {
            return R.drawable.app_on;
        } else if (tethering || internet) {
            return R.drawable.app_yellow;
        } else {
            return R.drawable.app_off;
        }
    }

    private Notification buildNotification(String caption, int icon) {
        lastNotificationTickerText = caption;
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exitIntent = new Intent(EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setTicker(caption)
                    .setContentText(caption)
                    .setContentTitle(getText(R.string.app_name))
                    .setOngoing(true)
                    .setSmallIcon(icon)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setStyle(new Notification.BigTextStyle().bigText(caption).setBigContentTitle(getText(R.string.app_name)));

            Intent onIntent = new Intent(TETHERING);
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(this, 0, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            int drawable = R.drawable.ic_service24;
            String ticker = "Service ON";

            if (forceOff && !forceOn) {
                drawable = R.drawable.ic_wifi_off;
                ticker = "Tethering OFF";
            } else if (forceOn && !forceOff) {
                drawable = R.drawable.ic_wifi_on;
                ticker = "Tethering ON";
            }

            builder.addAction(drawable, ticker, onPendingIntent);

            if (status == Status.DEACTIVATED_ON_IDLE) {
                Intent onResumeIntent = new Intent(RESUME);
                PendingIntent onResumePendingIntent = PendingIntent.getBroadcast(this, 0, onResumeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_resume24, "Resume", onResumePendingIntent);
            } else if (status == Status.DATA_USAGE_LIMIT_EXCEED) {
                Intent onLimitIntent = new Intent(TetherIntents.UNLOCK);
                PendingIntent onLimitPendingIntent = PendingIntent.getBroadcast(this, 0, onLimitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_unlocked, "Unlock", onLimitPendingIntent);
            }

            builder.addAction(R.drawable.ic_exit24, "Exit", exitPendingIntent);
            notify = builder.build();
        } else {
            notify = new Notification(icon, caption, System.currentTimeMillis());
            notify.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), caption, pendingIntent);
        }
        return notify;
    }

    private void updateNotification() {
        showNotification(lastNotificationTickerText, getNotificationIcon());
    }

    private void showNotification(String body, int icon) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = buildNotification(body, icon);
        notificationManager.cancelAll();
        notificationManager.notify(NOTIFICATION_ID, notification);
        MyLog.i(TAG, "Notification: " + body);
    }

    @Override
    public void onDestroy() {
        MyLog.i(TAG, "onDestroy");
        flag = false;
        revertToInitialState();
        stopForeground(true);
        stopSelf();
        unregisterReceiver(receiver);
        dataUsageTask.cancel();
        bluetoothTask.cancel();
        super.onDestroy();
    }

    private void revertToInitialStateAsync() {
        new TurnOnTetheringAsyncTask().doInBackground(false);
        new TurnOn3GAsyncTask().doInBackground(initial3GStatus);
        new TurnOnBTAsyncTask().doInBackground(initialBluetoothStatus);
    }

    private void revertToInitialState() {
        if (prefs.getBoolean(RETURN_TO_PREV_STATE, false) && prefs.getBoolean(ACTIVATE_KEEP_SERVICE, true)) {
            new TurnOn3GAsyncTask().doInBackground(initial3GStatus);
            new TurnOnTetheringAsyncTask().doInBackground(initialTetheredStatus);
        }
        if (initialWifiStatus) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    serviceHelper.enableWifi();
                }
            }, 1000);
        }
        new TurnOnBTAsyncTask().doInBackground(initialBluetoothStatus);
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MyLog.i(TAG, intent.getAction());
            switch (intent.getAction()) {
                case TETHERING:
                    // Correct order of execution: Turn OFF -> Turn ON -> Service ON -> ...
                    if (forceOn && !forceOff) {
                        // Turn OFF
                        forceOff = true;
                        forceOn = false;
                        blockForceInternet = false;
                        execute(TETHER_OFF);
                        execute(INTERNET_OFF);
                    } else if (!forceOff && !forceOn) {
                        // Turn ON
                        forceOn = true;
                        blockForceInternet = false;
                        execute(INTERNET_ON);
                        execute(TETHER_ON);
                    } else {
                        // Service ON
                        forceOff = false;
                        forceOn = false;
                        onService();
                        updateNotification();
                    }
                    break;

                case WIDGET:
                    changeMobileState = intent.getExtras().getBoolean("changeMobileState", false);
                    blockForceInternet = true;

                    if (serviceHelper.isTetheringWiFi()) {
                        forceOff = true;
                        forceOn = false;
                        execute(TETHER_OFF);
                    } else {
                        forceOn = true;
                        forceOff = false;
                        setStatus(Status.DEFAULT);
                        execute(TETHER_ON);
                    }

                    if (changeMobileState) {
                        forceInternetConnect();
                    }
                    break;

                case RESUME:
                    updateLastAccess();
                    connectedDeviceName = null;
                    setStatus(previousStatus);
                    onService();
                    updateNotification();
                    break;

                case USB_ON:
                    if (!forceOff) {
                        if (prefs.getBoolean("usb.activate.on.connect", false)) {
                            execute(TETHER_ON, R.string.activate_tethering_usb_on);
                        }
                        if (prefs.getBoolean("usb.internet.force.on", false)) {
                            execute(INTERNET_ON, R.string.activate_internet_usb_on);
                        }
                        setStatus(Status.USB_ON);
                    }
                    break;
                case USB_OFF:
                    if (prefs.getBoolean("usb.deactivate.on.disconnect", false)) {
                        execute(TETHER_OFF, R.string.activate_tethering_usb_off);
                    }
                    if (prefs.getBoolean("usb.internet.force.off", false)) {
                        execute(INTERNET_OFF, R.string.activate_internet_usb_off);
                    }
                    setStatus(Status.DEFAULT);
                    break;
                case BT_RESTORE:
                    if (!initialBluetoothStatus) {
                        BluetoothAdapter.getDefaultAdapter().disable();
                    }
                    connectedDeviceName = null;
                    revertToInitialStateAsync();
                    setStatus(Status.DEFAULT);
                    break;
                case BT_CONNECTED:
                    if (!forceOff) {
                        String deviceName = intent.getStringExtra("name");
                        connectedDeviceName = deviceName;
                        setStatus(Status.BT);
                        execute(BLUETOOTH_INTERNET_TETHERING_ON);
                    }
                    break;

                case BT_DISCONNECTED:
                    String deviceName = intent.getStringExtra("name");
                    if (connectedDeviceName != null && connectedDeviceName.equals(deviceName)) {
                        connectedDeviceName = null;
                    }
                    execute(BLUETOOTH_INTERNET_TETHERING_OFF);
                    break;

                case BT_SEARCH:
                    new BluetoothTask(getApplicationContext(), connectedDeviceName).execute();
                    break;

                case TEMPERATURE_ABOVE_LIMIT:
                    if (serviceHelper.isTetheringWiFi()) {
                        execute(TEMP_TETHERING_OFF);
                    }
                    break;

                case TEMPERATURE_BELOW_LIMIT:
                    if (status == Status.TEMPERATURE_OFF && !serviceHelper.isTetheringWiFi()) {
                        execute(TEMP_TETHERING_ON);
                    }
                    break;

                case EVENT_TETHER_OFF:
                    onTetheringOff();
                case EVENT_TETHER_ON:
                    onTetheringOn();
                case EVENT_MOBILE_OFF:
                    onMobileOff();
                case EVENT_MOBILE_ON:
                    onMobileOn();
                case EVENT_WIFI_OFF:
                    onWifiOff();
                case EVENT_WIFI_ON:
                    onWifiOn();
                    updateNotification();
                    break;

                case TetherIntents.TETHER_ON:
                    execute(TETHER_ON);
                    break;

                case TetherIntents.TETHER_OFF:
                    execute(TETHER_OFF);
                    break;

                case TetherIntents.INTERNET_ON:
                    execute(INTERNET_ON);
                    break;

                case TetherIntents.INTERNET_OFF:
                    execute(INTERNET_OFF);
                    break;

                case EXIT:
                    stopSelf();
                    break;
            }
        }

        private void forceInternetConnect() {
            if (forceOff) {
                execute(INTERNET_OFF);
            } else if (forceOn) {
                if (!allowRoaming()) {
                    showNotification(getString(R.string.roaming_service_disabled), R.drawable.app_off);
                    forceOff = true;
                    forceOn = false;
                } else {
                    execute(INTERNET_ON);
                }
            }
        }
    }

    private void onWifiOn() {
    }

    private void onWifiOff() {

    }

    private void onMobileOn() {
    }

    private void onMobileOff() {
    }

    private void onTetheringOn() {
        tetheringProcessing = false;
    }

    private void onTetheringOff() {
        tetheringProcessing = false;
        if (wifiWasEnabled && !serviceHelper.isWifiEnabled()) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (!serviceHelper.isTetheringWiFi()) {
                        serviceHelper.enableWifi();
                    }
                }
            });
        }
    }

    private void onService() {
        if (!tetheringProcessing) {
            boolean tethering = serviceHelper.isTetheringWiFi();
            boolean mobileOn = serviceHelper.isConnectedToInternetThroughMobile();

            if (isActivated3G() && !mobileOn) {
                execute(INTERNET_ON);
            } else if (internetOn && !isActivated3G() && mobileOn) {
                execute(INTERNET_OFF);
            }

            if (isActivatedTethering() && !tethering) {
                execute(TETHER_ON);
            } else if (tethering && !isActivatedTethering()) {
                execute(INTERNET_OFF);
            }
        }
    }

    private void execute(ServiceAction... serviceAction) {
        for (ServiceAction action : serviceAction) {
            execute(action, 0);
        }
    }

    private void execute(ServiceAction serviceAction, int msg) {
        boolean action = serviceAction.isOn();
        boolean showNotify = false;
        if (serviceAction.isInternet() && serviceHelper.isConnectedOrConnectingToInternet() != action) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                MyLog.d(TAG, "Current Android OS doesn't support turn mobile data!");
            } else if (!internetAsyncTask(action)) {
                return;
            }
            showNotify = true;
        }
        if (serviceAction.isTethering() && serviceHelper.isTetheringWiFi() != action) {
            if (!tetheringAsyncTask(action)) {
                return;
            }
            showNotify = true;
        }

        MyLog.i(TAG, "Execute action: " + serviceAction.toString());
        notify(serviceAction, msg, showNotify);
    }

    private void notify(ServiceAction serviceAction, int msg, boolean showNotify) {
        Status oldStatus = status;
        int id = R.string.service_started;
        int icon = getIcon(serviceAction);
        switch (serviceAction) {
            case TETHER_ON:
                updateLastAccess();
                id = R.string.notification_tethering_restored;
                setStatus(Status.DEFAULT);
                break;
            case TETHER_OFF:
                id = R.string.notification_tethering_off;
                break;
            case INTERNET_ON:
                if (!Utils.isAirplaneModeOn(getApplicationContext())) {
                    updateLastAccess();
                    setStatus(Status.DEFAULT);
                    id = R.string.notification_internet_restored;
                }
                break;
            case INTERNET_OFF:
                id = R.string.notification_internet_off;
                break;
            case SCHEDULED_TETHER_ON:
                updateLastAccess();
                id = R.string.notification_scheduled_tethering_on;
                setStatus(Status.ACTIVATED_ON_SCHEDULE);
                break;
            case SCHEDULED_TETHER_OFF:
                id = R.string.notification_scheduled_tethering_off;
                setStatus(Status.DEACTIVATED_ON_SCHEDULE);
                break;
            case SCHEDULED_INTERNET_ON:
                updateLastAccess();
                id = R.string.notification_scheduled_internet_on;
                setStatus(Status.ACTIVATED_ON_SCHEDULE);
                break;
            case SCHEDULED_INTERNET_OFF:
                id = R.string.notification_scheduled_internet_off;
                setStatus(Status.DEACTIVATED_ON_SCHEDULE);
                break;
            case TETHER_OFF_IDLE:
                id = R.string.notification_idle_tethering_off;
                previousStatus = status;
                setStatus(Status.DEACTIVATED_ON_IDLE);
                break;
            case INTERNET_OFF_IDLE:
                id = R.string.notification_idle_internet_off;
                previousStatus = status;
                setStatus(Status.DEACTIVATED_ON_IDLE);
                break;
            case DATA_USAGE_EXCEED_LIMIT:
                id = R.string.notification_data_exceed_limit;
                setStatus(Status.DATA_USAGE_LIMIT_EXCEED);
                break;
            case BLUETOOTH_INTERNET_TETHERING_ON:
                id = R.string.bluetooth_on;
                setStatus(Status.BT);
                break;
            case BLUETOOTH_INTERNET_TETHERING_OFF:
                id = R.string.bluetooth_off;
                setStatus(Status.DEFAULT);
                break;
            case CELL_INTERNET_TETHERING_ON:
                id = R.string.cell_on;
                setStatus(Status.ACTIVATED_ON_CELL);
                break;
            case CELL_INTERNET_TETHERING_OFF:
                id = R.string.cell_off;
                setStatus(Status.DEACTIVATED_ON_CELL);
                break;
            case TEMP_TETHERING_OFF:
                id = R.string.temp_off;
                setStatus(Status.TEMPERATURE_OFF);
                break;
            case TEMP_TETHERING_ON:
                id = R.string.temp_on;
                setStatus(Status.DEFAULT);
                break;
            default:
                MyLog.e(TAG, "Missing default notification!");
        }
        if (msg != 0) {
            id = msg;
        }

        if (showNotify || !status.equals(oldStatus)) {
            showNotification(getString(id), icon);
        }
    }

    //TODO replace with getNotificationIcon()
    private int getIcon(ServiceAction serviceAction) {
        int icon = R.drawable.app_off;
        if (serviceAction.name().contains("IDLE")) {
            icon = R.drawable.app_off;
        } else if (serviceHelper.isConnectedToInternetThroughMobile() && serviceHelper.isTetheringWiFi()) {
            icon = R.drawable.app_on;
        } else if (serviceHelper.isConnectedOrConnectingToInternet() || serviceHelper.isTetheringWiFi()) {
            icon = R.drawable.app_yellow;
        }
        return icon;
    }

    public void setStatus(Status status) {
        MyLog.d(TAG, "New status = " + status.name());
        previousStatus = this.status;
        this.status = status;
    }
}
