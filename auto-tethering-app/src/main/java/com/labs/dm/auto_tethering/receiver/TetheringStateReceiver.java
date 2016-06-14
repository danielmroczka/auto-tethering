package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.WidgetService;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

public class TetheringStateReceiver extends BroadcastReceiver {

    private final String TAG = "TetheringStateChange";

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        Log.i(TAG, intent.getAction() + " " + String.valueOf(helper.isSharingWiFi()).toUpperCase());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);

        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            Log.i(TAG, "widget id=" + widgetId);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getLayout(intent));
            Intent intent2 = new Intent(context, WidgetService.class);
            intent2.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getService(context, widgetId, intent2, PendingIntent.FLAG_ONE_SHOT);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private int getLayout(Intent intent) {
        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
        int layout = R.layout.widget_layout_wait;
        switch (state) {
            case 3:
            case 13:
                layout = R.layout.widget_layout_on;
                break;
            case 1:
            case 11:
                layout = R.layout.widget_layout_off;
                break;
        }
        return layout;
    }
}
