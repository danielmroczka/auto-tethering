package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.service.WidgetService;

/**
 * Created by daniel on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i("tetheringwidget", "onUpdate");

        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout_on);
            Intent intent = new Intent(context, TetheringWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        context.startService(new Intent(context, WidgetService.class));

        //IntentFilter filter = new IntentFilter();
        //filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        //ConnectionChangeReceiver mReceiver = new ConnectionChangeReceiver();
        //context.getApplicationContext().registerReceiver(mReceiver, filter);
    }

}
