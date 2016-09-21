package com.labs.dm.auto_tethering;

import android.app.Application;
import android.util.Log;

/**
 * Created by Daniel Mroczka on 2016-09-20.
 */

public class MyLog extends Application {

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }
}
