package com.labs.dm.auto_tethering.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Daniel Mroczka on 6/8/2016.
 */
public class BluetoothService extends IntentService {

    private static final String TAG = "BluetoothService";

    public BluetoothService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "onStart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerInvents() {

    }
}
