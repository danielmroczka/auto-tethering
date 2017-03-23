package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.TetherIntents;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

/**
 * Created by Daniel Mroczka
 */
public class WidgetService extends IntentService {

    private ServiceHelper serviceHelper;

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean state = serviceHelper.isTetheringWiFi();
        MyLog.i("WidgetService", "onHandleIntent, state=" + state + ", extras=" + intent.getExtras().toString());
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final int widgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);

        if (!serviceHelper.isServiceRunning(TetheringService.class) && prefs.getBoolean(key(widgetId, "start.service"), false)) {
            Intent serviceIntent = new Intent(this, TetheringService.class);
            startService(serviceIntent);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent onIntent = new Intent(TetherIntents.WIDGET);
                    onIntent.putExtra("changeMobileState", prefs.getBoolean(key(widgetId, "mobile"), false));
                    sendBroadcast(onIntent);
                }
            }, 250);
        } else if (serviceHelper.isServiceRunning(TetheringService.class)) {
            Intent onIntent = new Intent(TetherIntents.WIDGET);
            onIntent.putExtra("changeMobileState", prefs.getBoolean(key(widgetId, "mobile"), false));
            sendBroadcast(onIntent);
        } else {
            changeState(state, prefs, widgetId);
        }
    }

    private void changeState(boolean state, SharedPreferences prefs, int widgetId) {
        if (prefs.getBoolean("widget." + widgetId + ".mobile", false)) {
            internetAsyncTask(!state);
        }
        if (prefs.getBoolean("widget." + widgetId + ".tethering", true)) {
            tetheringAsyncTask(!state);
        }
    }

    private class TurnOnTetheringAsyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            serviceHelper.setWifiTethering(params[0], null);
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

    @Override
    public void onCreate() {
        super.onCreate();
        serviceHelper = new ServiceHelper(getApplicationContext());
    }

    private void tetheringAsyncTask(boolean state) {
        new TurnOnTetheringAsyncTask().doInBackground(state);
    }

    private void internetAsyncTask(boolean state) {
        new TurnOn3GAsyncTask().doInBackground(state);
    }

    private String key(int id, String key) {
        return "widget." + id + "." + key;
    }
}
