package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.service.ServiceHelper;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("CCR", intent.getAction());
        ServiceHelper helper = new ServiceHelper("", context);
        Log.i("CCR", String.valueOf(helper.isSharingWiFi()));

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isSharingWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);
            Intent intent2 = new Intent(context, TetheringWidgetProvider.class);
            intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
