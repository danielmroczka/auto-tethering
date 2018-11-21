package com.labs.dm.auto_tethering.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static com.labs.dm.auto_tethering.AppProperties.CHANNEL_ID;
import static com.labs.dm.auto_tethering.AppProperties.SILENT_CHANNEL_ID;

public class NotificationHelper {

    public static final void createChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            String silentChannelName = "Not intrusive Notification";
            NotificationChannel silentChannel = new NotificationChannel(SILENT_CHANNEL_ID, silentChannelName, NotificationManager.IMPORTANCE_LOW);
            silentChannel.enableVibration(prefs.getBoolean("vibrate.on.tethering", false));
            notificationManager.createNotificationChannel(silentChannel);

            String channelName = "Normal Notification";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(prefs.getBoolean("vibrate.on.tethering", false));
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
