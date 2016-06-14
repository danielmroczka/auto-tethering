package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.WidgetService;

import java.util.Arrays;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

/**
 * Created by Daniel Mroczka on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    private final String TAG = "Tethering Widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(TAG, "onUpdate");
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        ServiceHelper helper = new ServiceHelper(context);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isSharingWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);

            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getService(context, widgetId, intent, PendingIntent.FLAG_ONE_SHOT);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        for (String key : prefs.getAll().keySet()) {
            for (int id : appWidgetIds) {
                if (key.startsWith("widget." + id)) {
                    prefs.edit().remove(key).apply();
                }
            }
        }
        Log.i(TAG, "Remove widget ids: " + Arrays.toString(appWidgetIds));
        super.onDeleted(context, appWidgetIds);
    }
}
