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

public class TetheringStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        Log.i("CCR", intent.getAction() + " " + String.valueOf(helper.isSharingWiFi()).toUpperCase());

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);

        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isSharingWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);

            Intent intent2 = new Intent(context, WidgetService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent2, 0);

            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
