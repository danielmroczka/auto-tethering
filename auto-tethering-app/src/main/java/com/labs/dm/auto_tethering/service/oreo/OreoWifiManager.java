package com.labs.dm.auto_tethering.service.oreo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RequiresApi(api = Build.VERSION_CODES.O)
public class OreoWifiManager {

    /**
     * From {@link ConnectivityManager}
     */
    private static final int TETHERING_WIFI = 0;

    private static final String TAG = OreoWifiManager.class.getSimpleName();

    private Context mContext;

    public OreoWifiManager(Context c) {
        mContext = c;
    }

    public void startTethering(OnStartTetheringCallback callback, Handler handler) {

        CallbackMaker cm = new CallbackMaker(mContext, callback);
        Class<?> mSystemCallbackClazz = cm.getCallBackClass();
        Object mSystemCallback = null;
        try {
            Constructor constructor = mSystemCallbackClazz.getDeclaredConstructor(int.class);
            mSystemCallback = constructor.newInstance(0);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        ConnectivityManager manager = mContext.getApplicationContext().getSystemService(ConnectivityManager.class);
        Method method;
        Class callbackClass = null;
        try {
            try {
                callbackClass = Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            method = manager.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, callbackClass, Handler.class);

            if (method == null) {
                Log.e(TAG, "startTetheringMethod is null");
            } else {
                method.invoke(manager, TETHERING_WIFI, false, mSystemCallback, handler);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    public void stopTethering() {

        ConnectivityManager manager = mContext.getApplicationContext().getSystemService(ConnectivityManager.class);

        try {
            Method method = manager.getClass().getDeclaredMethod("stopTethering", int.class);

            if (method == null) {
                Log.e(TAG, "stopTetheringMethod is null");
            } else {
                method.invoke(manager, TETHERING_WIFI);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }


}
