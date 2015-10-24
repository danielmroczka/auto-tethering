package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import static com.labs.dm.auto_tethering.AppProperties.SCHEDULER;
import static com.labs.dm.auto_tethering.AppProperties.SIMCARD_LIST;
import static com.labs.dm.auto_tethering.AppProperties.TIME_OFF;
import static com.labs.dm.auto_tethering.AppProperties.TIME_ON;

/**
 * Created by Daniel Mroczka
 */
public class TetheringService extends IntentService {

    private static final String TAG = "AutoTetheringService";
    private final static int CHECK_DELAY = 15;
    private final int NOTIFICATION_SHORT = 0, NOTIFICATION_LONG = 1;
    private Calendar timeOff, timeOn;
    private SharedPreferences prefs;
    private long lastAccess = Calendar.getInstance().getTimeInMillis();
    private boolean initial3GStatus, initialTetheredStatus;
    private WifiManager wifiManager;

    public TetheringService() {
        super("AutoTetheringService");
    }

    public static WifiConfiguration getWifiApConfiguration(final Context ctx) {
        final WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        final Method m = getWifiManagerMethod("getWifiApConfiguration", wifiManager);
        if (m != null) {
            try {
                return (WifiConfiguration) m.invoke(wifiManager);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return null;
    }

    private static Method getWifiManagerMethod(final String methodName, final WifiManager wifiManager) {
        final Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    private void showNotification(String body, int id) {
        cancelNotification(NOTIFICATION_LONG);
        String subject = (String) getText(R.string.app_name);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify = new Notification(R.drawable.app, body, System.currentTimeMillis());
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notify.setLatestEventInfo(getApplicationContext(), subject, body, pending);
        notificationManager.notify(id, notify);
        Log.i(TAG, body);
    }

    private void cancelNotification(int id) {
        NotificationManager notif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notif.cancel(id);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        showNotification("Service started", NOTIFICATION_SHORT);

        while (true) {
            try {
                if (isServiceActived()) {
                    if (isCorrectSimCard()) {
                        if (checkForRoaming()) {

                            boolean connected3G = Utils.isConnected(getApplicationContext());
                            boolean tethered = isSharingWiFi();

                            if (isScheduledTimeOff()) {
                                if (connected3G) {
                                    internetAsyncTask(false);
                                    showNotification(getString(R.string.notification_scheduled_internet_off), NOTIFICATION_LONG);
                                }
                                if (tethered) {
                                    tetheringAsyncTask(false);
                                    showNotification(getString(R.string.notification_scheduled_tethering_off), NOTIFICATION_LONG);
                                }
                            } else if (checkIdle()) {
                                if (connected3G && check3GIdle()) {
                                    internetAsyncTask(false);
                                    showNotification(getString(R.string.notification_idle_internet_off), NOTIFICATION_LONG);
                                }
                                if (tethered && checkWifiIdle()) {
                                    tetheringAsyncTask(false);
                                    showNotification(getString(R.string.notification_idle_tethering_off), NOTIFICATION_LONG);
                                }
                            } else if (updateStatus()) {
                                if (isActivated3G() && !connected3G) {
                                    internetAsyncTask(true);
                                    showNotification(getString(R.string.notification_internet_restored), NOTIFICATION_SHORT);
                                } else if (!isActivated3G() && connected3G) {
                                    internetAsyncTask(false);
                                    showNotification(getString(R.string.notification_internet_off), NOTIFICATION_SHORT);
                                }
                                if (isActivatedTethering() && !tethered) {
                                    tetheringAsyncTask(true);
                                    showNotification(getString(R.string.notification_tethering_restored), NOTIFICATION_SHORT);
                                } else if (!isActivatedTethering() && tethered) {
                                    tetheringAsyncTask(false);
                                    showNotification(getString(R.string.notification_tethering_off), NOTIFICATION_SHORT);
                                }
                            }
                        }
                    }
                }

                TimeUnit.SECONDS.sleep(CHECK_DELAY);
                cancelNotification(NOTIFICATION_SHORT);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }

        }
    }

    private Void tetheringAsyncTask(boolean state) {
        return new TurnOnTetheringAsyncTask().doInBackground(state);
    }

    private boolean updateStatus() {
        return true;
    }

    private boolean isServiceActived() {
        return true;
    }

    private boolean isScheduledTimeOff() {
        Calendar c = Calendar.getInstance();
        onChangeProperties();
        timeOn.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        timeOff.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        if (isSchedulerOn()) {
            if (c.after(timeOff) && c.before(timeOn)) {
                return true;
            }
        }

        return false;
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
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        onChangeProperties();
        init();
    }

    private void init() {
        initial3GStatus = Utils.isConnected(getApplicationContext());
        initialTetheredStatus = isSharingWiFi();
    }

    private void onChangeProperties() {
        DateFormat formatter = new SimpleDateFormat("HH:mm");

        timeOff = Calendar.getInstance();
        timeOn = Calendar.getInstance();
        try {
            timeOff.setTime(formatter.parse(prefs.getString(TIME_OFF, "")));
            timeOn.setTime(formatter.parse(prefs.getString(TIME_ON, "")));
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void internetAsyncTask(boolean state) {
        new TurnOn3GAsyncTask().doInBackground(state);
    }

    private boolean isSharingWiFi() {
        try {
            final Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (IllegalAccessException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (InvocationTargetException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, ex.getMessage());
        }

        return false;
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
            String simCard = tMgr.getSimSerialNumber();
            return simCard != null && Utils.exists(prefs.getString(SIMCARD_LIST, ""), "");
        } else {
            return true;
        }
    }

    protected class TurnOn3GAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            Utils.setMobileDataEnabled(getApplicationContext(), params[0]);
            return null;
        }
    }

    protected class TurnOnTetheringAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            Utils.setWifiTetheringEnabled(getApplicationContext(), wifiManager, params[0]);
            return null;
        }
    }
}
