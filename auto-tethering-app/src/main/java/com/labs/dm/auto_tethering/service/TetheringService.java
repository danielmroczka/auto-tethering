package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherInvent;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.Cron.STATUS;
import com.labs.dm.auto_tethering.db.DBManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_KEEP_SERVICE;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_ROAMING;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_SIMCARD;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.DEFAULT_IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.FORCE_NET_FROM_NOTIFY;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.RETURN_TO_PREV_STATE;
import static com.labs.dm.auto_tethering.Utils.adapterDayOfWeek;
import static com.labs.dm.auto_tethering.service.ServiceAction.BLUETOOTH_INTERNET_TETHERING_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.DATA_USAGE_EXCEED_LIMIT;
import static com.labs.dm.auto_tethering.service.ServiceAction.INTERNET_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.INTERNET_OFF_IDLE;
import static com.labs.dm.auto_tethering.service.ServiceAction.INTERNET_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_INTERNET_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_INTERNET_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_TETHER_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.SCHEDULED_TETHER_ON;
import static com.labs.dm.auto_tethering.service.ServiceAction.SIMCARD_BLOCK;
import static com.labs.dm.auto_tethering.service.ServiceAction.TETHER_OFF;
import static com.labs.dm.auto_tethering.service.ServiceAction.TETHER_OFF_IDLE;
import static com.labs.dm.auto_tethering.service.ServiceAction.TETHER_ON;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private boolean forceOff = false, forceOn = false;
    private boolean changeMobileState;
    private BroadcastReceiver receiver;
    private String lastNotifcationTickerText;
    private List<String> devices = new ArrayList<>();
    private BluetoothDevice connectedDevice;

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
    private boolean initial3GStatus, initialTetheredStatus, initialBluetoothStatus;
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

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TetherInvent.TETHERING);
        filter.addAction(TetherInvent.WIDGET);
        filter.addAction(TetherInvent.RESUME);
        filter.addAction(TetherInvent.EXIT);
        filter.addAction(TetherInvent.USB_ON);
        filter.addAction(TetherInvent.USB_OFF);
        filter.addAction(TetherInvent.BT_FOUND_NEW);
        filter.addAction(TetherInvent.BT_FOUND_START);
        filter.addAction(TetherInvent.BT_FOUND_END);
        filter.addAction(TetherInvent.BT_SET_IDLE);
        filter.addAction(TetherInvent.BT_BONDED);
        filter.addAction(TetherInvent.BT_DISCONNECTED);

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
        initialTetheredStatus = serviceHelper.isSharingWiFi();
        initialBluetoothStatus = serviceHelper.isBluetoothActive();
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
            sendBroadcast(new Intent(TetherInvent.USB_ON));
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

            if (!checkForRoaming()) {
                showNotification(getString(R.string.roaming_service_disabled));
            }
        }

        while (flag) {
            try {
                if (!(forceOff || forceOn) && (isServiceActivated() || keepService())) {
                    if (enabled()) {
                        boolean connected3G = serviceHelper.isConnectedToInternet();
                        boolean tethered = serviceHelper.isSharingWiFi();
                        boolean idle = checkIdle();
                        ScheduleResult res = scheduler();
                        if (res == ScheduleResult.OFF) {
                            execute(SCHEDULED_INTERNET_OFF);
                            execute(SCHEDULED_TETHER_OFF);
                        } else if (res == ScheduleResult.ON) {
                            status = Status.DEFAULT;
                            if (isActivated3G()) {
                                execute(SCHEDULED_INTERNET_ON);
                            }
                            execute(SCHEDULED_TETHER_ON);
                        } else if (idle) {
                            if (check3GIdle()) {
                                execute(INTERNET_OFF_IDLE);
                            }
                            if (checkWifiIdle()) {
                                execute(TETHER_OFF_IDLE);
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
        return isCorrectSimCard() && checkForRoaming();
    }

    private boolean batteryLevel() {
        boolean batteryLevel = prefs.getBoolean("usb.off.battery.lvl", false);
        return !batteryLevel || (batteryLevel && 100f * serviceHelper.batteryLevel() >= Integer.valueOf(prefs.getString("usb.off.battery.lvl.value", "15")));
    }

    private boolean usb() {
        boolean flag = prefs.getBoolean("usb.only.when.connected", false);
        return (!flag || (flag && serviceHelper.isPluggedToPower())) && batteryLevel();
    }

    private boolean keepService() {
        return prefs.getBoolean(AppProperties.ACTIVATE_KEEP_SERVICE, true);
    }

    private void tetheringAsyncTask(boolean state) {
        if (serviceHelper.isSharingWiFi() == state) {
            return;
        }

        if (!state || status != Status.DEACTIVATED_ON_IDLE) {
            new TurnOnTetheringAsyncTask().doInBackground(state);
        }
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
        Intent onIntent = new Intent(TetherInvent.CLIENTS);
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

    private void internetAsyncTask(boolean state) {
        if (serviceHelper.isConnectedToInternet() == state) {
            return;
        }

        if (!state || status != Status.DEACTIVATED_ON_IDLE) {
            new TurnOn3GAsyncTask().doInBackground(state);
        }
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(ACTIVATE_TETHERING, false) && usb();
    }

    private boolean isActivated3G() {
        return prefs.getBoolean(ACTIVATE_3G, false) && usb();
    }

    private boolean checkForRoaming() {
        return !(!prefs.getBoolean(ACTIVATE_ON_ROAMING, false) && ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).isNetworkRoaming());
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

    private class FindAvailableBluetoothDevicesAsyncTask extends AsyncTask<BluetoothDevice, Void, Void> {

        @Override
        protected Void doInBackground(BluetoothDevice... devices) {
            boolean found = false;
            if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }

            List<BluetoothDevice> devicesToCheck = new ArrayList<>();

            if (devices != null && devices.length == 0) {
                List<String> preferredDevices = findPreferredDevices();
                for (BluetoothDevice device : serviceHelper.getBondedDevices()) {
                    for (String pref : preferredDevices) {
                        if (device.getName().equals(pref)) {
                            devicesToCheck.add(device);
                        }
                    }
                }
            } else {
                devicesToCheck.add(devices[0]);
            }

            for (BluetoothDevice device : devicesToCheck) {
                Log.d("BT Socket", "Connecting to " + device.getName());
                try {
                    Method method = device.getClass().getMethod("getUuids");
                    ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(device);
                    BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(parcelUuids[0].getUuid());
                    socket.connect();
                    Log.d("BT Socket", "Connected  to " + device.getName());
                    socket.close();
                    found = true;
                    connectedDevice = device;
                } catch (Exception e) {
                    connectedDevice = null;
                    Log.e(TAG, device.getName() + "Device is not in range");
                }
                if (found) {
                    sendBroadcast(new Intent(TetherInvent.BT_BONDED));
                    return null;
                }
            }

            return null;
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
            showNotification(getString(R.string.service_started));
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String caption) {
        lastNotifcationTickerText = caption;
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exitIntent = new Intent(TetherInvent.EXIT);
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

            Intent onIntent = new Intent(TetherInvent.TETHERING);
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
                Intent onResumeIntent = new Intent(TetherInvent.RESUME);
                PendingIntent onResumePendingIntent = PendingIntent.getBroadcast(this, 0, onResumeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_resume24, "Resume", onResumePendingIntent);
            } else if (status == Status.DATA_USAGE_LIMIT_EXCEED) {
                Intent onLimitIntent = new Intent(TetherInvent.UNLOCK);
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
                case TetherInvent.TETHERING:
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
                        showNotification(lastNotifcationTickerText);
                    }

                    if (prefs.getBoolean(FORCE_NET_FROM_NOTIFY, true)) {
                        forceInternetConnect();
                    }
                    break;

                case TetherInvent.RESUME:
                    updateLastAccess();
                    status = Status.DEFAULT;
                    break;

            }

            if (TetherInvent.WIDGET.equals(intent.getAction())) {
                changeMobileState = intent.getExtras().getBoolean("changeMobileState", false);

                if (serviceHelper.isSharingWiFi()) {
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
            }


            if (TetherInvent.USB_ON.equals(intent.getAction())) {
                if (prefs.getBoolean("usb.activate.on.connect", false)) {
                    execute(TETHER_ON, R.string.activate_tethering_usb_on);
                }
                if (prefs.getBoolean("usb.internet.force.on", false)) {
                    execute(INTERNET_ON, R.string.activate_internet_usb_on);
                }
                status = Status.USB_ON;
            }
            if (TetherInvent.USB_OFF.equals(intent.getAction())) {
                if (prefs.getBoolean("usb.activate.off.connect", false)) {
                    execute(TETHER_OFF, R.string.activate_tethering_usb_off);
                }
                if (prefs.getBoolean("usb.internet.force.off", false)) {
                    execute(INTERNET_OFF, R.string.activate_internet_usb_off);
                }
                status = Status.DEFAULT;
            }

            if (TetherInvent.BT_FOUND_START.equals(intent.getAction())) {
                devices.clear();
            }
            if (TetherInvent.BT_FOUND_NEW.equals(intent.getAction())) {
                devices.add(intent.getStringExtra("device"));
            }
            if (TetherInvent.BT_FOUND_END.equals(intent.getAction())) {
                boolean found = false;
                List<String> preferredDevices = findPreferredDevices();
                for (String deviceName : devices) {
                    for (String preferredDevice : preferredDevices) {
                        if (deviceName != null && preferredDevice != null && deviceName.equals(preferredDevice)) {
                            status = Status.BT;
                            execute(BLUETOOTH_INTERNET_TETHERING_ON);
                            found = true;
                            break;
                        }
                    }
                }

                if (status == Status.BT && connectedDevice != null) {
                    new FindAvailableBluetoothDevicesAsyncTask().doInBackground(connectedDevice);
                } else {
                    new FindAvailableBluetoothDevicesAsyncTask().doInBackground();
                }

                if (!found && status == Status.BT) {
                    if (prefs.getBoolean("bt.internet.restore.to.initial", false) && serviceHelper.isConnectedToInternet() && !initial3GStatus) {
                        execute(INTERNET_OFF);
                    }
                    status = Status.DEFAULT;
                }
            }
            if (TetherInvent.BT_SET_IDLE.equals(intent.getAction())) {
                if (!initialBluetoothStatus && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    BluetoothAdapter.getDefaultAdapter().disable();
                }
            }

            if (TetherInvent.BT_BONDED.equals(intent.getAction())) {
                status = Status.BT;
                execute(BLUETOOTH_INTERNET_TETHERING_ON);
            }

            if (TetherInvent.EXIT.equals(intent.getAction())) {
                stopSelf();
            }
        }

        private void forceInternetConnect() {
            if (forceOff) {
                execute(INTERNET_OFF);
            } else if (forceOn) {
                if (!checkForRoaming()) {
                    showNotification(getString(R.string.roaming_service_disabled));
                    forceOff = true;
                    forceOn = false;
                } else {
                    execute(INTERNET_ON);
                }
            }
        }
    }

    private List<String> findPreferredDevices() {
        Map<String, ?> map = prefs.getAll();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().startsWith("bt.devices.")) {
                list.add(String.valueOf(entry.getValue()));
            }
        }
        return list;
    }

    private void execute(ServiceAction serviceAction) {
        execute(serviceAction, 0);
    }

    private void execute(ServiceAction serviceAction, int msg) {
        boolean action = serviceAction.isOn();
        Status oldStatus = status;
        boolean showNotify = false;
        if (serviceAction.isInternet() && serviceHelper.isConnectedToInternet() != action) {
            internetAsyncTask(action);
            showNotify = true;
        }
        if (serviceAction.isTethering() && serviceHelper.isSharingWiFi() != action) {
            tetheringAsyncTask(action);
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
                id = R.string.notification_data_execced_limit;
                status = Status.DATA_USAGE_LIMIT_EXCEED;
                break;
            case BLUETOOTH_INTERNET_TETHERING_ON:
                id = R.string.bluetooth;
                status = Status.BT;
                break;
        }
        if (msg != 0) {
            id = msg;
        }

        if (showNotify || status != oldStatus) {
            showNotification(getString(id));
        }
    }
}
