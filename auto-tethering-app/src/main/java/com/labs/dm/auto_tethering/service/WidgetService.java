package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.labs.dm.auto_tethering.receiver.TetheringWidgetProvider;

public class WidgetService extends IntentService {

    private ServiceHelper serviceHelper;

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //updateWidget(serviceHelper.isSharingWiFi());
        boolean state = serviceHelper.isSharingWiFi();
        Log.i("WidgetService", "onHandleIntent, state=" + state);

        Intent serviceIntent = new Intent(this, TetheringService.class);
        stopService(serviceIntent);

        tetheringAsyncTask(!state);
        updateWidget(!state);
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
//        for (int widgetId : allWidgetIds) {
//
////            Intent serviceIntent = new Intent(getApplicationContext(), WidgetService.class);
////            PendingIntent pendingServiceIntent = PendingIntent.getService(getApplicationContext(), 0, serviceIntent, 0);
////            RemoteViews remoteView1 = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout_off);
////            RemoteViews remoteView2 = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout_on);
////            remoteView1.setOnClickPendingIntent(R.id.widget_button, pendingServiceIntent);
////            remoteView2.setOnClickPendingIntent(R.id.widget_button, pendingServiceIntent);
////            appWidgetManager.updateAppWidget(widgetId, remoteView1);
////            appWidgetManager.updateAppWidget(widgetId, remoteView2);
//
//
//            RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), state ? R.layout.widget_layout_on : R.layout.widget_layout_off);
//            Intent intent = new Intent(getApplicationContext(), WidgetService.class);
//            //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
//            //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
//
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
//            appWidgetManager.updateAppWidget(widgetId, remoteViews);
//        }

        return false;
    }
}
