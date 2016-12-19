package com.labs.dm.auto_tethering.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.service.ServiceHelper;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;

/**
 * Created by Daniel Mroczka
 */
public class TetheringStateReceiver extends BroadcastReceiver {

    private final String TAG = "TetheringStateChange";

    @Override
    public void onReceive(Context context, Intent intent) {
        ServiceHelper helper = new ServiceHelper(context);
        MyLog.i(TAG, intent.getAction() + " " + String.valueOf(helper.isTetheringWiFi()).toUpperCase());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, TetheringWidgetProvider.class);

        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            MyLog.i(TAG, "widget id=" + widgetId);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getLayout(intent));
            Intent widgetIntent = new Intent(context, TetheringWidgetProvider.class);
            widgetIntent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            widgetIntent.setAction("widget.click");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, widgetIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        context.getSharedPreferences("widget", 0).edit().putInt("clicks", 0).apply();
        vibrate(context);
        context.sendBroadcast(new Intent(TetherIntents.CHANGE_NETWORK_STATE));
    }

    private void vibrate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("vibrate.on.tethering", false)) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(200);
            }
        }
    }

    private int getLayout(Intent intent) {
        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

        int layout = R.layout.widget_layout_wait;
        switch (state % 10) {
            case WIFI_STATE_ENABLED:
                layout = R.layout.widget_layout_on;
                break;
            case WIFI_STATE_DISABLED:
                layout = R.layout.widget_layout_off;
                break;
        }
        return layout;
    }
}
