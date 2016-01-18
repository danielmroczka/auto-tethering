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
import com.labs.dm.auto_tethering.service.WidgetService;

/**
 * Created by daniel on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i("Tethering widget", "onUpdate");
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        ServiceHelper helper = new ServiceHelper("", context);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isSharingWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);

            Intent intent = new Intent(context, WidgetService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

            remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
