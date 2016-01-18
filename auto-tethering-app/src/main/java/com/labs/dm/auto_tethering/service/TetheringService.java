package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import com.labs.dm.auto_tethering.db.DBManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_3G;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_ROAMING;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_ON_SIMCARD;
import static com.labs.dm.auto_tethering.AppProperties.ACTIVATE_TETHERING;
import static com.labs.dm.auto_tethering.AppProperties.DEFAULT_IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.RETURN_TO_PREV_STATE;
import static com.labs.dm.auto_tethering.AppProperties.SCHEDULER;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private static final String TAG = "AutoTetheringService";
    private final static int CHECK_DELAY = 5;
    private Calendar timeOff, timeOn;
    private SharedPreferences prefs;
    private long lastAccess = Calendar.getInstance().getTimeInMillis();
    private boolean initial3GStatus, initialTetheredStatus;
    private ServiceHelper serviceHelper;
    private boolean runFromActivity;
    private boolean flag = true;
    private Notification notification;
    private final int NOTIFICATION_ID = 1234;
    private Intent intent;

    private boolean triggeredFromWidget;

    public TetheringService() {
        super(TAG);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        runFromActivity = intent.getBooleanExtra(("runFromActivity"), false);
        int state = intent.getIntExtra("state", -1);
        if (state == 1) {
            Log.i(TAG, "shouldOff");
            tetheringAsyncTask(false);
            showNotification(getString(R.string.notification_tethering_off));
            triggeredFromWidget = true;
        } else if (state == 0) {
            Log.i(TAG, "shouldOn");
            tetheringAsyncTask(true);
            showNotification(getString(R.string.notification_tethering_restored));
            triggeredFromWidget = true;
        }

        this.intent = intent;
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
                if (isServiceActivated()) {
                    if (isCorrectSimCard()) {
                        if (checkForRoaming()) {
                            boolean connected3G = serviceHelper.checkMobileConnection();
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
                                showNotification(getString(R.string.notification_idle_internet_off));
                            } else if (idle && tethered && checkWifiIdle()) {
                                tetheringAsyncTask(false);
                                showNotification(getString(R.string.notification_idle_tethering_off));
                            } else if (updateStatus()) {
                                if (isActivated3G() && !connected3G) {
                                    internetAsyncTask(true);
                                    showNotification(getString(R.string.notification_internet_restored));
                                } else if (!isActivated3G() && connected3G) {
                                    internetAsyncTask(false);
                                    showNotification(getString(R.string.notification_internet_off));
                                }
                                if (isActivatedTethering() && !tethered) {
                                    tetheringAsyncTask(true);
                                    showNotification(getString(R.string.notification_tethering_restored));
                                } else if (!isActivatedTethering() && tethered) {
                                    tetheringAsyncTask(false);
                                    showNotification(getString(R.string.notification_tethering_off));
                                }
                            }
                        }
                    }
                }

                TimeUnit.SECONDS.sleep(CHECK_DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void tetheringAsyncTask(boolean state) {
        new TurnOnTetheringAsyncTask().doInBackground(state);
    }

    private boolean updateStatus() {
        return true;
    }

    private boolean isServiceActivated() {
        return runFromActivity || prefs.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, false);
    }

    private boolean isScheduledTimeOff() {
        Calendar now = Calendar.getInstance();
        onChangeProperties();
        timeOn.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        timeOff.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        return isSchedulerOn() && now.after(timeOff) && now.before(timeOn);

    }

    private boolean checkIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false) || prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (Utils.connectedClients() > 0) {
                lastAccess = Calendar.getInstance().getTimeInMillis();
                return false;
            }

            return true;
        } else {
            lastAccess = Calendar.getInstance().getTimeInMillis();
        }
        return false;
    }

    private boolean check3GIdle() {
        if (prefs.getBoolean(IDLE_3G_OFF, false)) {
            if (Calendar.getInstance().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_3G_OFF_TIME, "60")) * 1000 * 60) {
                return true;
            }
        }

        return false;
    }

    private boolean checkWifiIdle() {
        if (prefs.getBoolean(IDLE_TETHERING_OFF, false)) {
            if (Calendar.getInstance().getTimeInMillis() - lastAccess > Integer.valueOf(prefs.getString(IDLE_TETHERING_OFF_TIME, DEFAULT_IDLE_TETHERING_OFF_TIME)) * 1000 * 60) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceHelper = new ServiceHelper(TAG, getApplicationContext());
        onChangeProperties();
        init();
        runAsForeground();
    }

    private void init() {
        initial3GStatus = serviceHelper.checkMobileConnection();
        initialTetheredStatus = serviceHelper.isSharingWiFi();
    }

    private void onChangeProperties() {
        DateFormat formatter = new SimpleDateFormat("HH:mm");

        timeOff = Calendar.getInstance();
        timeOn = Calendar.getInstance();
        try {
            Cron cron = DBManager.getInstance(getApplicationContext()).getCron();
            if (cron != null) {
                if (cron.getTimeOff() != null) {
                    timeOff.setTime(formatter.parse(cron.getTimeOff()));
                }
                if (cron.getTimeOn() != null) {
                    timeOn.setTime(formatter.parse(cron.getTimeOn()));
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void internetAsyncTask(boolean state) {
        new TurnOn3GAsyncTask().doInBackground(state);
    }

    private boolean isActivatedTethering() {
        return prefs.getBoolean(ACTIVATE_TETHERING, false);
    }

    private boolean isSchedulerOn() {
        return prefs.getBoolean(SCHEDULER, false);
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
        Notification notify = new Notification(R.drawable.app, getText(R.string.service_started), System.currentTimeMillis());
        this.notification = notify;
        showNotification("Service started");
        startForeground(NOTIFICATION_ID, notify);
    }

    private void showNotification(String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), body, pending);
        notification.when = System.currentTimeMillis();
        notification.tickerText = body;
        notificationManager.notify(NOTIFICATION_ID, notification);
        Log.i(TAG, "Notification: " + body);
    }

    @Override
    public void onDestroy() {
        flag = false;
        revertToInitialState();
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    private void revertToInitialState() {
        if (prefs.getBoolean(RETURN_TO_PREV_STATE, false)) {
            serviceHelper.setMobileDataEnabled(initial3GStatus);
            serviceHelper.setWifiTethering(initialTetheredStatus);
        }
    }
}
