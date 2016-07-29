package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.activity.ConfigurationActivity;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.WidgetService;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

/**
 * Created by Daniel Mroczka on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    private static final int DOUBLE_CLICK_DELAY = 250;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        ServiceHelper helper = new ServiceHelper(context);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isTetheringWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);
        Intent intent = new Intent(context, getClass());
        intent.setAction("Click");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
        context.getSharedPreferences("widget", 0).edit().putInt("clicks", 0).commit();

    }

    public static int getWidgetId(Intent intent) {
        Bundle extras = intent.getExtras();
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        return appWidgetId;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (intent.getAction().equals("Click")) {

            int clickCount = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("clicks", 0);
            context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks", ++clickCount).commit();

            final Handler handler = new Handler() {
                public void handleMessage(Message msg) {

                    int clickCount = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("clicks", 0);

                    if (clickCount > 1) {
                        Intent i = new Intent(context, ConfigurationActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                        Log.i("Widget", "Count>1");

                        Toast.makeText(context, "doubleClick", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent i = new Intent(context, WidgetService.class);
                        i.putExtra(EXTRA_APPWIDGET_ID, getWidgetId(intent));
                        // i.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startService(i);

//                        Log.i(TAG, "onUpdate");
//                        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
//                        ServiceHelper helper = new ServiceHelper(context);
//                        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
//                        for (int widgetId : allWidgetIds) {
//                            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isTetheringWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);
//
//                            Intent intent = new Intent(context, WidgetService.class);
//                            intent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
//                            PendingIntent pendingIntent = PendingIntent.getService(context, widgetId, intent, PendingIntent.FLAG_ONE_SHOT);
//                            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
//                            appWidgetManager.updateAppWidget(widgetId, remoteViews);
//                        }


                        Log.i("Widget", "Count==1");
                        Toast.makeText(context, "singleClick", Toast.LENGTH_SHORT).show();
                    }

                    context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks", 0).commit();
                }
            };

            if (clickCount == 1) new Thread() {
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            wait(DOUBLE_CLICK_DELAY);
                        }
                        handler.sendEmptyMessage(0);
                    } catch (InterruptedException ex) {
                    }
                }
            }.start();
        }

        super.onReceive(context, intent);

    }
}