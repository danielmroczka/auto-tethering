package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.DBManager;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.labs.dm.auto_tethering.AppProperties.*;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private boolean forceOff = false, forceOn = false;
    boolean changeMobileState;
    private BroadcastReceiver receiver;
    private String lastNotifcationTickerText;

    private enum Status {
        DEACTIVED_ON_IDLE, DEFAULT
    }

    private static final String TAG = "AutoTetheringService";
    private final static int CHECK_DELAY = 5;
    private List<Cron> crons;
    private SharedPreferences prefs;
    private long lastAccess = Calendar.getInstance().getTimeInMillis();
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
        receiver = new MyBroadcastReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        runAsForeground();
    }

    private void init() {
        initial3GStatus = serviceHelper.isMobileConnectionActive();
        initialTetheredStatus = serviceHelper.isSharingWiFi();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        runFromActivity = intent.getBooleanExtra("runFromActivity", false);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (isServiceActivated()) {
            showNotification(getString(R.string.service_started));

            if (!isCorrectSimCard()) {
                internetAsyncTask(false);
                tetheringAsyncTask(false);
                showNotification(getString(R.string.inserted_blocked_simcard));
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
                            boolean connected3G = serviceHelper.isMobileConnectionActive();
                            boolean tethered = serviceHelper.isSharingWiFi();
                            boolean idle = checkIdle();

                            if (isScheduledTimeOff()) {
                                if (connected3G) {
                                    internetAsyncTask(false);
                                    showNotification(getString(R.string.notification_scheduled_internet_off));
                                }
                                if (tethered) {
                                    tetheringAsyncTask(false);
                                    showNotification(getString(R.string.notification_scheduled_tethering_off));
                                }
                            } else if (idle && connected3G && check3GIdle()) {
                                internetAsyncTask(false);
                                status = Status.DEACTIVED_ON_IDLE;
                                showNotification(getString(R.string.notification_idle_internet_off));
                            } else if (idle && tethered && checkWifiIdle()) {
                                tetheringAsyncTask(false);
                                status = Status.DEACTIVED_ON_IDLE;
                                showNotification(getString(R.string.notification_idle_tethering_off));
                            } else {
                                if (isActivated3G() && !connected3G) {
                                    if (internetAsyncTask(true)) {
                                        showNotification(getString(R.string.notification_internet_restored));
                                    }
                                } else if (!isActivated3G() && connected3G) {
                                    internetAsyncTask(false);
                                    showNotification(getString(R.string.notification_internet_off));
                                }
                                if (isActivatedTethering() && !tethered) {
                                    if (tetheringAsyncTask(true)) {
                                        showNotification(getString(R.string.notification_tethering_restored));
                                    }
                                } else if (!isActivatedTethering() && tethered) {
                                    tetheringAsyncTask(false);
                                    showNotification(getString(R.string.notification_tethering_off));
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

    private boolean tetheringAsyncTask(boolean state) {
        if (serviceHelper.isSharingWiFi() == state) {
            return false;
        }
        //showNotification(getString(state ? R.string.notification_tethering_restored : R.string.notification_tethering_off));
        if (!state || status != Status.DEACTIVED_ON_IDLE) {
            new TurnOnTetheringAsyncTask().doInBackground(state);
            return true;
        }
        return false;
    }

    private boolean isServiceActivated() {
        return runFromActivity || prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false);
    }

    private boolean isScheduledTimeOff() {
        Calendar now = Calendar.getInstance();
        onChangeProperties();
        boolean state = false;
        for (Cron cron : crons) {
            Calendar timeOn = Calendar.getInstance();
            timeOn.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), cron.getHourOn(), cron.getMinOn(), 0);

            Calendar timeOff = Calendar.getInstance();
            timeOff.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), cron.getHourOff(), cron.getMinOff(), 0);

            boolean matchedMask = (cron.getMask() & (int) Math.pow(2, Utils.adapterDayOfWeek(now.get(Calendar.DAY_OF_WEEK)))) > 0;

            boolean active = cron.getStatus() == Cron.STATUS.SCHED_OFF_ENABLED.getValue();

            state = state || (active && timeOff.getTimeInMillis() < now.getTimeInMillis() && now.getTimeInMillis() < timeOn.getTimeInMillis() && matchedMask);
        }

        return state;
    }

    /**
     * Returns true when idle settings are switched on and no client is connected
     *
     * @return
     */
    private boolean checkIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false) || prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (Utils.connectedClients() > 0) {
                lastAccess = Calendar.getInstance().getTimeInMillis();
                status = Status.DEFAULT;
                return false;
            }

            return true;
        } else {
            lastAccess = Calendar.getInstance().getTimeInMillis();
            status = Status.DEFAULT;
        }
        return false;
    }

    private boolean check3GIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false)) {
            if (Calendar.getInstance().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_3G_OFF_TIME, "60")) * 1000 * 60) {
                return true;
            }
            status = Status.DEFAULT;
        }

        return false;
    }

    private boolean checkWifiIdle() {
        if (prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (Calendar.getInstance().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_TETHERING_OFF_TIME, DEFAULT_IDLE_TETHERING_OFF_TIME)) * 1000 * 60) {
                return true;
            }
            status = Status.DEFAULT;
        }
        return false;
    }

    private void onChangeProperties() {
        crons = DBManager.getInstance(getApplicationContext()).getCrons();
    }

    private boolean internetAsyncTask(boolean state) {
        if (serviceHelper.isMobileConnectionActive() == state) {
            return false;
        }
        //showNotification(getString(state ? R.string.notification_internet_restored : R.string.notification_internet_off));
        if (!state || status != Status.DEACTIVED_ON_IDLE) {
            new TurnOn3GAsyncTask().doInBackground(state);
            return true;
        }
        return false;
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
            this.notification = buildNotification("Service started");
            showNotification("Service started");
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String caption) {
        lastNotifcationTickerText = caption;
        Notification notify;
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exitIntent = new Intent("exit");
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(caption)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.app)
                    .setAutoCancel(false)
                    .setTicker(caption)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setWhen(System.currentTimeMillis());

            if (status == Status.DEACTIVED_ON_IDLE) {
                Intent onResumeIntent = new Intent("resume");
                PendingIntent onResumePendingIntent = PendingIntent.getBroadcast(this, 0, onResumeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_cancel, "Resume", onResumePendingIntent);
            } else {
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
            }

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
                    showNotification(getString(R.string.notification_tethering_off));
                    tetheringAsyncTask(false);
                } else if (!forceOff && !forceOn) {
                    forceOn = true;
                    showNotification(getString(R.string.notification_tethering_restored));
                    tetheringAsyncTask(true);
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
                    showNotification(getString(R.string.notification_tethering_off));
                    tetheringAsyncTask(false);
                } else {
                    forceOn = true;
                    forceOff = false;
                    showNotification(getString(R.string.notification_tethering_restored));
                    tetheringAsyncTask(true);
                }

                if (changeMobileState) {
                    forceInternetConnect();
                }
            }

            if ("resume".equals(intent.getAction())) {
                lastAccess = Calendar.getInstance().getTimeInMillis();
                status = Status.DEFAULT;
            }
        }

        private void forceInternetConnect() {
            if (forceOff) {
                showNotification(getString(R.string.notification_internet_off));
                internetAsyncTask(false);
            } else if (forceOn) {
                if (!checkForRoaming()) {
                    showNotification(getString(R.string.roaming_service_disabled));
                    forceOff = true;
                    forceOn = false;
                } else {
                    showNotification(getString(R.string.notification_internet_restored));
                    internetAsyncTask(true);
                }
            }
        }
    }
}
