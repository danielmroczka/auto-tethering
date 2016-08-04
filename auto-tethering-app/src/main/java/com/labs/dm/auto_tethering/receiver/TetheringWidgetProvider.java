package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.activity.ConfigurationActivity;
import com.labs.dm.auto_tethering.service.ServiceHelper;
import com.labs.dm.auto_tethering.service.WidgetService;

import java.util.Arrays;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.content.Context.MODE_PRIVATE;
import static com.labs.dm.auto_tethering.Utils.getWidgetId;

/**
 * Created by Daniel Mroczka on 2015-12-23.
 */
public class TetheringWidgetProvider extends AppWidgetProvider {

    private static final int DOUBLE_CLICK_DELAY = 800;
    private static final String TAG = "WidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        ServiceHelper helper = new ServiceHelper(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), helper.isTetheringWiFi() ? R.layout.widget_layout_on : R.layout.widget_layout_off);
            Intent intent = new Intent(context, getClass());
            intent.setAction("widget.click");
            intent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        context.getSharedPreferences("widget", 0).edit().putInt("clicks", 0).commit();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals("widget.click")) {
            int clickCount = context.getSharedPreferences("widget", MODE_PRIVATE).getInt("clicks", 0);
            context.getSharedPreferences("widget", MODE_PRIVATE).edit().putInt("clicks", ++clickCount).commit();

            final Handler handler = new Handler() {
                public void handleMessage(Message msg) {
                    int clickCount = context.getSharedPreferences("widget", MODE_PRIVATE).getInt("clicks", 0);
                    Log.i(TAG, "ClickCount: " + clickCount);

                    if (clickCount > 1) {
                        Intent i = new Intent(context, ConfigurationActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra(EXTRA_APPWIDGET_ID, getWidgetId(intent));
                        i.putExtra("editMode", true);
                        context.startActivity(i);
                    } else {
                        Intent i = new Intent(context, WidgetService.class);
                        i.putExtra(EXTRA_APPWIDGET_ID, getWidgetId(intent));
                        context.startService(i);
                    }

                    context.getSharedPreferences("widget", MODE_PRIVATE).edit().putInt("clicks", 0).commit();
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
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }.start();
        }
        super.onReceive(context, intent);
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