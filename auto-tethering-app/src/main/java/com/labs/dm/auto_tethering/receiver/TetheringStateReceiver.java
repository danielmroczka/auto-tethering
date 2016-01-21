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
import com.labs.dm.auto_tethering.service.WidgetService;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

public class TetheringStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        Log.i("TetheringStateChange", intent.getAction() + " " + String.valueOf(helper.isSharingWiFi()).toUpperCase());

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);

        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            Log.i("TetheringStateChange", "widget id" + widgetId);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isSharingWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);

            Intent intent2 = new Intent(context, WidgetService.class);
            intent2.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent2, PendingIntent.FLAG_ONE_SHOT);

            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
