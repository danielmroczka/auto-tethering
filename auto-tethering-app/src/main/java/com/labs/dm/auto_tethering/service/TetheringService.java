package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.Cron.STATUS;
import com.labs.dm.auto_tethering.db.DBManager;

import java.util.Calendar;
import java.util.List;
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
import static com.labs.dm.auto_tethering.service.ServiceAction.*;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private boolean forceOff = false, forceOn = false;
    private boolean changeMobileState;
    private BroadcastReceiver receiver;
    private String lastNotifcationTickerText;

    private enum Status {
        DEACTIVED_ON_IDLE, ACTIVATED_ON_SCHEDULE, DEFAULT
    }

    private static final String TAG = "AutoTetheringService";
    private final static int CHECK_DELAY = 5;
    private List<Cron> crons;
    private SharedPreferences prefs;
    private long lastAccess = getTime().getTimeInMillis();
    private boolean initial3GStatus, initialTetheredStatus;
    private ServiceHelper serviceHelper;
    private boolean runFromActivity;
    private boolean flag = true;
    private Notification notification;
    private final int NOTIFICATION_ID = 1234;

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
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("tethering");
        filter.addAction("widget");
        filter.addAction("resume");
        filter.addAction("exit");
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
                if (forceOff || forceOn) {
                    continue;
                }
                if (isServiceActivated() || keepService()) {
                    if (isCorrectSimCard()) {
                        if (checkForRoaming()) {
                            boolean connected3G = serviceHelper.isConnectedToInternet();
                            boolean tethered = serviceHelper.isSharingWiFi();
                            boolean idle = checkIdle();
                            ScheduleResult res = scheduler();
                            if (res == ScheduleResult.OFF) {
                                execute(SCHEDULED_INTERNET_OFF);
                                execute(SCHEDULED_TETHER_OFF);
                            } else if (res == ScheduleResult.ON) {
                                if (isActivated3G()) {
                                    execute(SCHEDULED_INTERNET_ON);
                                }
                                execute(SCHEDULED_TETHER_ON);
                            } else if (idle && check3GIdle()) {
                                execute(INTERNET_OFF_IDLE);
                            } else if (idle && checkWifiIdle()) {
                                execute(TETHER_OFF_IDLE);
                            } else {
                                if (isActivated3G() && !connected3G) {
                                    execute(INTERNET_ON);
                                } else if (!isActivated3G() && connected3G && status == Status.DEFAULT) {
                                    execute(INTERNET_OFF);
                                }
                                if (isActivatedTethering() && !tethered) {
                                    execute(TETHER_ON);
                                } else if (!isActivatedTethering() && tethered && status == Status.DEFAULT) {
                                    execute(TETHER_OFF);
                                }
                            }
                        }
                    }
                }
                if (!keepService()) {
                    flag = false;
                }

                TimeUnit.SECONDS.sleep(CHECK_DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private boolean keepService() {
        return prefs.getBoolean(AppProperties.ACTIVATE_KEEP_SERVICE, true);
    }

    private void tetheringAsyncTask(boolean state) {
        if (serviceHelper.isSharingWiFi() == state) {
            return;
        }

        if (!state || status != Status.DEACTIVED_ON_IDLE) {
            new TurnOnTetheringAsyncTask().doInBackground(state);
        }
    }

    private boolean isServiceActivated() {
        return runFromActivity || prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false);
    }

    private enum ScheduleResult {
        ON, OFF, NONE
    }

    /**
     * Iterates through all cron items.
     *
     * @return ScheduleResult
     */
    private ScheduleResult scheduler() {
        Calendar now = getTime();
        onChangeProperties();
        boolean state = false;
        for (Cron cron : crons) {
            Calendar timeOff = getTime();
            adjustCalendar(timeOff, cron.getHourOff(), cron.getMinOff());
            Calendar timeOn = getTime();
            adjustCalendar(timeOn, cron.getHourOn(), cron.getMinOn());

            boolean matchedMask = (cron.getMask() & (int) Math.pow(2, adapterDayOfWeek(now.get(Calendar.DAY_OF_WEEK)))) > 0;
            boolean active = cron.getStatus() == STATUS.SCHED_OFF_ENABLED.getValue();
            boolean scheduled = timeOff.getTimeInMillis() < now.getTimeInMillis() && now.getTimeInMillis() < timeOn.getTimeInMillis();

            if (active && matchedMask && cron.getHourOff() == -1) {
                long diff = now.getTimeInMillis() - timeOn.getTimeInMillis();
                if (diff > 0 && CHECK_DELAY * 1000 >= diff) {
                    return ScheduleResult.ON;
                }
                continue;
            }
            if (active && matchedMask && cron.getHourOn() == -1) {
                long diff = now.getTimeInMillis() - timeOff.getTimeInMillis();
                if (diff > 0 && CHECK_DELAY * 1000 >= diff) {
                    return ScheduleResult.OFF;
                }
                continue;
            }

            state = state || (active && scheduled && matchedMask);


        }

        return state ? ScheduleResult.OFF : ScheduleResult.NONE;
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
        if (prefs.getBoolean(IDLE_3G_OFF, false) || prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (Utils.connectedClients() > 0) {
                lastAccess = getTime().getTimeInMillis();
                status = Status.DEFAULT;
                return false;
            }

            return true;
        } else {
            lastAccess = getTime().getTimeInMillis();
            if (status == Status.DEACTIVED_ON_IDLE) {
                status = Status.DEFAULT;
            }
        }
        return false;
    }

    private boolean check3GIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false)) {
            if (getTime().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_3G_OFF_TIME, "60")) * 1000 * 60) {
                return true;
            }
            status = Status.DEFAULT;
        }

        return false;
    }

    private boolean checkWifiIdle() {
        if (prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (getTime().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_TETHERING_OFF_TIME, DEFAULT_IDLE_TETHERING_OFF_TIME)) * 1000 * 60) {
                return true;
            }
            status = Status.DEFAULT;
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

        if (!state || status != Status.DEACTIVED_ON_IDLE) {
            new TurnOn3GAsyncTask().doInBackground(state);
        }
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(ACTIVATE_TETHERING, false);
    }

    private boolean isActivated3G() {
        return prefs.getBoolean(ACTIVATE_3G, false);
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
        if (caption.equals(lastNotifcationTickerText)) {

        }
        lastNotifcationTickerText = caption;
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exitIntent = new Intent("exit");
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.app_name))
                    //.setSubText("subtext")
                    .setTicker(caption)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.app)
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX);
            builder.setStyle(new Notification.BigTextStyle(builder)
                    .bigText(caption)
                    .setBigContentTitle(getText(R.string.app_name)));
            //.setSummaryText(Formatter.formatShortFileSize(getApplicationContext(), TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes())));
            Intent onIntent = new Intent("tethering");
            PendingIntent onPendingIntent = PendingIntent.getBroadcast(this, 0, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            int drawable = R.drawable.ic_service;
            String ticker = "Service on";

            if (forceOff && !forceOn) {
                drawable = R.drawable.ic_wifi_off24;
                ticker = "Tethering OFF";
            } else if (forceOn && !forceOff) {
                drawable = R.drawable.ic_wifi_on24;
                ticker = "Tethering ON";
            }

            builder.addAction(drawable, ticker, onPendingIntent);

            if (status == Status.DEACTIVED_ON_IDLE) {
                Intent onResumeIntent = new Intent("resume");
                PendingIntent onResumePendingIntent = PendingIntent.getBroadcast(this, 0, onResumeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_resume, "Resume", onResumePendingIntent);
            }

            builder.addAction(R.drawable.ic_exit, "Close", exitPendingIntent);
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
        flag = false;
        revertToInitialState();
        stopForeground(true);
        stopSelf();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void revertToInitialState() {
        if (prefs.getBoolean(RETURN_TO_PREV_STATE, false) && prefs.getBoolean(ACTIVATE_KEEP_SERVICE, true)) {
            serviceHelper.setMobileDataEnabled(initial3GStatus);
            serviceHelper.setWifiTethering(initialTetheredStatus);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("tethering".equals(intent.getAction())) {
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
            }

            if ("widget".equals(intent.getAction())) {
                changeMobileState = intent.getExtras().getBoolean("changeMobileState", false);

                if (serviceHelper.isSharingWiFi()) {
                    forceOff = true;
                    forceOn = false;
                    execute(TETHER_OFF);
                } else {
                    forceOn = true;
                    forceOff = false;
                    execute(TETHER_ON);
                }

                if (changeMobileState) {
                    forceInternetConnect();
                }
            }

            if ("resume".equals(intent.getAction())) {
                lastAccess = getTime().getTimeInMillis();
                status = Status.DEFAULT;
            }

            if ("exit".equals(intent.getAction())) {
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

    private void execute(ServiceAction serviceAction) {
        boolean action = serviceAction.isOn();
        boolean showNotify = false;
        if (serviceAction.isInternet() && serviceHelper.isConnectedToInternet() != action) {
            internetAsyncTask(action);
            showNotify = true;
        }
        if (serviceAction.isTethering() && serviceHelper.isSharingWiFi() != action) {
            tetheringAsyncTask(action);
            showNotify = true;
        }

        if (showNotify) {
            Log.i(TAG, "Execute action: " + serviceAction.toString());
            int id = R.string.service_started;

            switch (serviceAction) {
                case TETHER_ON:
                    id = R.string.notification_tethering_restored;
                    break;
                case TETHER_OFF:
                    id = R.string.notification_tethering_off;
                    break;
                case INTERNET_ON:
                    id = R.string.notification_internet_restored;
                    break;
                case INTERNET_OFF:
                    id = R.string.notification_internet_off;
                    break;
                case SCHEDULED_TETHER_ON:
                    id = R.string.notification_scheduled_tethering_on;
                    status = Status.ACTIVATED_ON_SCHEDULE;
                    break;
                case SCHEDULED_TETHER_OFF:
                    id = R.string.notification_scheduled_tethering_off;
                    break;
                case SCHEDULED_INTERNET_ON:
                    id = R.string.notification_scheduled_internet_on;
                    status = Status.ACTIVATED_ON_SCHEDULE;
                    break;
                case SCHEDULED_INTERNET_OFF:
                    id = R.string.notification_scheduled_internet_off;
                    break;
                case TETHER_OFF_IDLE:
                    id = R.string.notification_idle_tethering_off;
                    status = Status.DEACTIVED_ON_IDLE;
                    break;
                case INTERNET_OFF_IDLE:
                    id = R.string.notification_idle_internet_off;
                    status = Status.DEACTIVED_ON_IDLE;
                    break;
            }
            showNotification(getString(id));
        }
    }
}
