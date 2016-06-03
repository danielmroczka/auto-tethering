package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

public class WidgetService extends IntentService {

    private ServiceHelper serviceHelper;

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean state = serviceHelper.isSharingWiFi();
        Log.i("WidgetService", "onHandleIntent, state=" + state + ", extras=" + intent.getExtras().toString());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int widgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1);

        if (!serviceHelper.isServiceRunning(TetheringService.class) && prefs.getBoolean(key(widgetId, "start.service"), false)) {
            Intent serviceIntent = new Intent(this, TetheringService.class);
            startService(serviceIntent);
            //TODO Remove sleep
            try {
                TimeUnit.MILLISECONDS.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent onIntent = new Intent("widget");
            onIntent.putExtra("changeMobileState", prefs.getBoolean(key(widgetId, "mobile"), false));
            sendBroadcast(onIntent);
        } else if (serviceHelper.isServiceRunning(TetheringService.class)) {
            Intent onIntent = new Intent("widget");
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
            serviceHelper.setWifiTethering(params[0]);
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
