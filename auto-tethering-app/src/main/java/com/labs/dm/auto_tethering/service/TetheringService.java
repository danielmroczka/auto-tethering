package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
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
import static com.labs.dm.auto_tethering.AppProperties.*;
import static com.labs.dm.auto_tethering.TetherIntents.*;
import static com.labs.dm.auto_tethering.Utils.adapterDayOfWeek;
import static com.labs.dm.auto_tethering.service.ServiceAction.*;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private boolean forceOff = false, forceOn = false;
    private boolean changeMobileState;
    private BroadcastReceiver receiver;
    private String lastNotificationTickerText;
    private String connectedDeviceName;

    private enum ScheduleResult {
        ON, OFF, NONE
    }

    private enum Status {
        DEACTIVATED_ON_IDLE,
        ACTIVATED_ON_SCHEDULE,
        DEACTIVATED_ON_SCHEDULE,
        USB_ON,
        DATA_USAGE_LIMIT_EXCEED,
        BT,
        DEFAULT
    }

    private static final String TAG = "TetheringService";
    private final static int CHECK_DELAY = 5;
    private List<Cron> crons;
    private SharedPreferences prefs;
    private long lastAccess = getTime().getTimeInMillis();
    private boolean initial3GStatus, initialTetheredStatus, initialBluetoothStatus, initialWiFiStatus;
    private ServiceHelper serviceHelper;
    private boolean runFromActivity;
    private boolean flag = true;
    private Notification notification;
    private final int NOTIFICATION_ID = 1234;
    private Timer timer;
    private TimerTask dataUsageTask, bluetoothTask;

    private Status status = Status.DEFAULT;

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

    private final String[] invents = {TetherIntents.TETHERING, TetherIntents.WIDGET, TetherIntents.RESUME, EXIT, TetherIntents.USB_ON, TetherIntents.USB_OFF,
            TetherIntents.BT_RESTORE, TetherIntents.BT_CONNECTED, BT_DISCONNECTED, BT_SEARCH};

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
        runAsForeground();
    }

    private void init() {
        initial3GStatus = serviceHelper.isConnectedToInternet();
        initialTetheredStatus = serviceHelper.isTetheringWiFi();
        initialBluetoothStatus = serviceHelper.isBluetoothActive();
        initialWiFiStatus = serviceHelper.isWiFiEnabled();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        runFromActivity = intent.getBooleanExtra("runFromActivity", false);
        int state = intent.getIntExtra("state", -1);
        if (state == 1) {
            execute(TETHER_OFF);
        } else if (state == 0) {
            execute(TETHER_ON);
        }
        if (intent.getBooleanExtra("usb.on", false)) {
            sendBroadcast(new Intent(TetherIntents.USB_ON));
        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (isServiceActivated()) {
            showNotification(getString(R.string.service_started));

            if (!isCorrectSimCard()) {
                execute(SIMCARD_BLOCK);
            }

            if (!allowRoaming()) {
                showNotification(getString(R.string.roaming_service_disabled));
            }
        }

        while (flag) {
            try {
                boolean connected3G = serviceHelper.isConnectedToInternet();
                boolean tethered = serviceHelper.isTetheringWiFi();

                if (!(forceOff || forceOn) && (isServiceActivated() || keepService())) {
                    if (enabled()) {
                        boolean idle = checkIdle();
                        ScheduleResult res = scheduler();
                        if (res == ScheduleResult.OFF) {
                            execute(SCHEDULED_INTERNET_OFF, SCHEDULED_TETHER_OFF);
                        } else if (res == ScheduleResult.ON) {
                            status = Status.DEFAULT;
                            if (isActivated3G()) {
                                execute(SCHEDULED_INTERNET_ON);
                            }
                            execute(SCHEDULED_TETHER_ON);
                        } else if (idle && serviceHelper.isTetheringWiFi()) {
                            if (check3GIdle() && serviceHelper.isConnectedToInternet()) {
                                execute(INTERNET_OFF_IDLE);
                            }
                            if (checkWifiIdle() && serviceHelper.isTetheringWiFi()) {
                                execute(TETHER_OFF_IDLE);
                            }
                        } else if (status == Status.DEACTIVATED_ON_IDLE && !idle) {
                            if (isActivated3G() && !connected3G) {
                                status = Status.DEFAULT;
                                execute(INTERNET_ON);
                            }
                        }

                        if (status == Status.DEFAULT) {
                            if (isActivated3G() && !connected3G) {
                                execute(INTERNET_ON);
                            } else if (!isActivated3G() && connected3G) {
                                execute(INTERNET_OFF);
                            }
                            if (isActivatedTethering() && !tethered) {
                                execute(TETHER_ON);
                            } else if (!isActivatedTethering() && tethered) {
                                execute(TETHER_OFF);
                            }
                        }
                    } else {
                        if (tethered || connected3G) {
                            execute(ROAMING_OFF, R.string.roaming_service_disabled);
                        }
                    }
                } else if (forceOn) {
                    if (!tethered) {
                        execute(TETHER_ON);
                    }
                    if (!connected3G) {
                        execute(INTERNET_ON);
                    }
                }
                if (!keepService()) {
                    flag = false;
                }

                long usage = ServiceHelper.getDataUsage() - prefs.getLong("data.usage.reset.value", 0);

                if (prefs.getBoolean("data.limit.on", false)) {
                    if (usage / (1048576f) > Integer.parseInt(prefs.getString("data.limit.value", "0"))) {
                        execute(DATA_USAGE_EXCEED_LIMIT);
                    }
                } else {
                    if (status == Status.DATA_USAGE_LIMIT_EXCEED) {
                        status = Status.DEFAULT;
                    }
                }

                TimeUnit.SECONDS.sleep(CHECK_DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private boolean enabled() {
        return isCorrectSimCard() && allowRoaming();
    }

    private boolean batteryLevel() {
        boolean batteryLevel = prefs.getBoolean("usb.off.battery.lvl", false);
        return !batteryLevel || 100f * serviceHelper.batteryLevel() >= Integer.valueOf(prefs.getString("usb.off.battery.lvl.value", "15"));
    }

    private boolean usb() {
        boolean flag = prefs.getBoolean("usb.only.when.connected", false);
        return (!flag || serviceHelper.isPluggedToPower()) && batteryLevel();
    }

    private boolean keepService() {
        return prefs.getBoolean(AppProperties.ACTIVATE_KEEP_SERVICE, true);
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
            return false;
        }

        if (!state || status != Status.DEACTIVATED_ON_IDLE) {
            new TurnOnTetheringAsyncTask().doInBackground(state);
            return true;
        }

        return false;
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
                status = Status.DEFAULT;
            }
        }
        return false;
    }

    private void updateLastAccess() {
        lastAccess = getTime().getTimeInMillis();
    }

    private boolean check3GIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false)) {
            if (getTime().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_3G_OFF_TIME, "60")) * 1000 * 60) {
                return true;
            }
        }

        return false;
    }

    private boolean checkWifiIdle() {
        if (prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (getTime().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_TETHERING_OFF_TIME, DEFAULT_IDLE_TETHERING_OFF_TIME)) * 1000 * 60) {
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
        if (serviceHelper.isConnectedToInternet() == state) {
            return false;
        }

        if (state && Utils.isAirplaneModeOn(getApplicationContext())) {
            return false;
        }

        if (!state || status != Status.DEACTIVATED_ON_IDLE) {
            new TurnOn3GAsyncTask().doInBackground(state);
            return true;
        }

        return false;
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(ACTIVATE_TETHERING, false) && usb();
    }

    private boolean isActivated3G() {
        return prefs.getBoolean(ACTIVATE_3G, false) && usb();
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
            serviceHelper.setMobileDataEnabled(params[0]);
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
            //showNotification(getString(R.string.service_started));
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String caption) {
        lastNotificationTickerText = caption;
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exitIntent = new Intent(EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(caption)
                    .setTicker(caption)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.app)
                    //.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.app))
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setStyle(new Notification.BigTextStyle().bigText(caption));

            Intent onIntent = new Intent(TetherIntents.TETHERING);
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(this, 0, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            int drawable = R.drawable.ic_service24;
            String ticker = "Service on";

            if (forceOff && !forceOn) {
                drawable = R.drawable.ic_wifi_off24;
                ticker = "Tethering OFF";
            } else if (forceOn && !forceOff) {
                drawable = R.drawable.ic_wifi_on24;
                ticker = "Tethering ON";
            }

            builder.addAction(drawable, ticker, onPendingIntent);

            if (status == Status.DEACTIVATED_ON_IDLE) {
                Intent onResumeIntent = new Intent(TetherIntents.RESUME);
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
            notify = new Notification(R.drawable.app, caption, System.currentTimeMillis());
            notify.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), caption, pendingIntent);
        }
        return notify;
    }

    private void showNotification(String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = buildNotification(body);
        notificationManager.notify(NOTIFICATION_ID, notification);
        Log.i(TAG, "Notification: " + body);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        flag = false;
        revertToInitialState();
        stopForeground(true);
        stopSelf();
        unregisterReceiver(receiver);
        dataUsageTask.cancel();
        bluetoothTask.cancel();
        super.onDestroy();
    }

    private void revertToInitialState() {
        if (prefs.getBoolean(RETURN_TO_PREV_STATE, false) && prefs.getBoolean(ACTIVATE_KEEP_SERVICE, true)) {
            serviceHelper.setMobileDataEnabled(initial3GStatus);
            serviceHelper.setWifiTethering(initialTetheredStatus);
        }
        serviceHelper.setBluetoothStatus(initialBluetoothStatus);
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, intent.getAction());
            switch (intent.getAction()) {
                case TetherIntents.TETHERING:
                    if (forceOn && !forceOff) {
                        forceOff = true;
                        forceOn = false;
                        execute(TETHER_OFF);
                    } else if (!forceOff && !forceOn) {
                        forceOn = true;
                        execute(TETHER_ON);
                    } else {
                        forceOff = false;
                        forceOn = false;
                        showNotification(lastNotificationTickerText);
                    }

                    if (prefs.getBoolean(FORCE_NET_FROM_NOTIFY, true)) {
                        forceInternetConnect();
                    }
                    break;

                case TetherIntents.RESUME:
                    updateLastAccess();
                    status = Status.DEFAULT;
                    break;

                case TetherIntents.WIDGET:
                    changeMobileState = intent.getExtras().getBoolean("changeMobileState", false);

                    if (serviceHelper.isTetheringWiFi()) {
                        forceOff = true;
                        forceOn = false;
                        execute(TETHER_OFF);
                    } else {
                        forceOn = true;
                        forceOff = false;
                        status = Status.DEFAULT;
                        execute(TETHER_ON);
                    }

                    if (changeMobileState) {
                        forceInternetConnect();
                    }
                    break;

                case TetherIntents.USB_ON:
                    if (prefs.getBoolean("usb.activate.on.connect", false)) {
                        execute(TETHER_ON, R.string.activate_tethering_usb_on);
                    }
                    if (prefs.getBoolean("usb.internet.force.on", false)) {
                        execute(INTERNET_ON, R.string.activate_internet_usb_on);
                    }
                    status = Status.USB_ON;
                    break;
                case TetherIntents.USB_OFF:
                    if (prefs.getBoolean("usb.activate.off.connect", false)) {
                        execute(TETHER_OFF, R.string.activate_tethering_usb_off);
                    }
                    if (prefs.getBoolean("usb.internet.force.off", false)) {
                        execute(INTERNET_OFF, R.string.activate_internet_usb_off);
                    }
                    status = Status.DEFAULT;
                    break;
                case TetherIntents.BT_RESTORE:
                    if (!initialBluetoothStatus) {
                        BluetoothAdapter.getDefaultAdapter().disable();
                    }
                    connectedDeviceName = null;
                    status = Status.DEFAULT;
                    break;
                case TetherIntents.BT_CONNECTED:
                    String deviceName = intent.getStringExtra("name");
                    connectedDeviceName = deviceName;
                    status = Status.BT;
                    execute(BLUETOOTH_INTERNET_TETHERING_ON);
                    break;

                case BT_DISCONNECTED:
                    deviceName = intent.getStringExtra("name");
                    if (connectedDeviceName != null && connectedDeviceName.equals(deviceName)) {
                        connectedDeviceName = null;
                    }
                    execute(BLUETOOTH_INTERNET_TETHERING_OFF);
                    break;

                case BT_SEARCH:
                    new BluetoothTask(getApplicationContext(), prefs, connectedDeviceName, initialBluetoothStatus).execute();
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
                    showNotification(getString(R.string.roaming_service_disabled));
                    forceOff = true;
                    forceOn = false;
                } else {
                    execute(INTERNET_ON);
                }
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
        Status oldStatus = status;
        boolean showNotify = false;
        if (serviceAction.isInternet() && serviceHelper.isConnectedToInternet() != action) {
            if (!internetAsyncTask(action)) {
                return;
            }
            showNotify = true;
        }
        if (serviceAction.isTethering() && prefs.getBoolean("wifi.disable.disconnect", false) && serviceHelper.isWiFiConnected()) {
            showNotification("Tethering disabled because connected to WiFi network");
        } else if (serviceAction.isTethering() && serviceHelper.isTetheringWiFi() != action) {
            if (action && serviceHelper.isWiFiEnabled()) {
                initialWiFiStatus = serviceHelper.isWiFiEnabled();
                serviceHelper.setWifiStatus(false);
            }
            if (!tetheringAsyncTask(action)) {
                return;
            }
            showNotify = true;
        }

        Log.i(TAG, "Execute action: " + serviceAction.toString());
        int id = R.string.service_started;

        switch (serviceAction) {
            case TETHER_ON:
                updateLastAccess();
                id = R.string.notification_tethering_restored;
                status = Status.DEFAULT;
                break;
            case TETHER_OFF:
                if (!serviceHelper.isTetheringWiFi() && initialWiFiStatus && !serviceHelper.isWiFiEnabled()) {
                    serviceHelper.setWifiStatus(true);
                }
                id = R.string.notification_tethering_off;
                break;
            case INTERNET_ON:
                updateLastAccess();
                status = Status.DEFAULT;
                id = R.string.notification_internet_restored;
                break;
            case INTERNET_OFF:
                id = R.string.notification_internet_off;
                break;
            case SCHEDULED_TETHER_ON:
                updateLastAccess();
                id = R.string.notification_scheduled_tethering_on;
                status = Status.ACTIVATED_ON_SCHEDULE;
                break;
            case SCHEDULED_TETHER_OFF:
                id = R.string.notification_scheduled_tethering_off;
                status = Status.DEACTIVATED_ON_SCHEDULE;
                break;
            case SCHEDULED_INTERNET_ON:
                updateLastAccess();
                id = R.string.notification_scheduled_internet_on;
                status = Status.ACTIVATED_ON_SCHEDULE;
                break;
            case SCHEDULED_INTERNET_OFF:
                id = R.string.notification_scheduled_internet_off;
                status = Status.DEACTIVATED_ON_SCHEDULE;
                break;
            case TETHER_OFF_IDLE:
                id = R.string.notification_idle_tethering_off;
                status = Status.DEACTIVATED_ON_IDLE;
                break;
            case INTERNET_OFF_IDLE:
                id = R.string.notification_idle_internet_off;
                status = Status.DEACTIVATED_ON_IDLE;
                break;
            case DATA_USAGE_EXCEED_LIMIT:
                id = R.string.notification_data_exceed_limit;
                status = Status.DATA_USAGE_LIMIT_EXCEED;
                break;
            case BLUETOOTH_INTERNET_TETHERING_ON:
                id = R.string.bluetooth;
                status = Status.BT;
                break;
            case BLUETOOTH_INTERNET_TETHERING_OFF:
                id = R.string.bluetooth_off;
                status = Status.DEFAULT;
                break;
        }
        if (msg != 0) {
            id = msg;
        }

        if (showNotify || status != oldStatus) {
            showNotification(getString(id));
        }

    }

    private void showToast(final String text) {
        Handler h = new Handler(getApplication().getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(), text, Toast.LENGTH_LONG).show();
            }
        });
    }
}
