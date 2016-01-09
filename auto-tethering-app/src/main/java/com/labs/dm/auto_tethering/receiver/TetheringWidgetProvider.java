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
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.TetheringService;

/**
 * Created by daniel on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    @Override

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ServiceHelper helper = new ServiceHelper("", context);
        boolean state = helper.isSharingWiFi();
        Log.i("autowifi", "onUpdate");

        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), state ? R.layout.widget_layout_off : R.layout.widget_layout_on);

            Intent intent = new Intent(context, TetheringWidgetProvider.class);

            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        Intent intent = new Intent(context.getApplicationContext(), TetheringService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        intent.putExtra("state", state ? 1 : 0);

        context.startService(intent);
    }

}
