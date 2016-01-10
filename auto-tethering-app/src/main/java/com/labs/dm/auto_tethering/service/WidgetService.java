package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.receiver.TetheringWidgetProvider;

public class WidgetService extends IntentService {

    private ServiceHelper serviceHelper;

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("WidgetService", "ON");
        updateWidget(serviceHelper.isSharingWiFi());
        int state = intent.getIntExtra("state", -1);
        if (state == 1) {
            tetheringAsyncTask(false);
            updateWidget(false);
            Intent serviceIntent = new Intent(this, TetheringService.class);
            stopService(serviceIntent);
        } else if (state == 0) {
            tetheringAsyncTask(true);
            updateWidget(true);
            Intent serviceIntent = new Intent(this, TetheringService.class);
            stopService(serviceIntent);
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
        serviceHelper = new ServiceHelper("Widget", getApplicationContext());
    }

    private void tetheringAsyncTask(boolean state) {
        new TurnOnTetheringAsyncTask().doInBackground(state);
    }

    private void internetAsyncTask(boolean state) {
        new TurnOn3GAsyncTask().doInBackground(state);
    }

    private boolean updateWidget(boolean state) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
        ComponentName thisWidget = new ComponentName(getApplicationContext(), TetheringWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        Log.i("check", "check" + state);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), state ? R.layout.widget_layout_on : R.layout.widget_layout_off);
            Intent intent = new Intent(getApplicationContext(), TetheringWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        return false;
    }
}
