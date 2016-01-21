package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.labs.dm.auto_tethering.receiver.TetheringWidgetProvider;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

public class WidgetService extends IntentService {

    private ServiceHelper serviceHelper;

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean state = serviceHelper.isSharingWiFi();
        Log.i("WidgetService", "onHandleIntent, state=" + state);
        updateWidget();
        Intent serviceIntent = new Intent(this, TetheringService.class);
        stopService(serviceIntent);

        int widgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (prefs.getBoolean("widget." + widgetId + ".mobile", false)) {
            internetAsyncTask(!state);
        }
        if (prefs.getBoolean("widget." + widgetId + ".tethering", true)) {
            tetheringAsyncTask(!state);
        }
    }

    private void updateWidget() {
        ComponentName thisWidget = new ComponentName(getApplicationContext(), TetheringWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            //  RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout_wait);
            //  appWidgetManager.updateAppWidget(widgetId, remoteViews);
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
}
