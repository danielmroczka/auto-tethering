package com.labs.dm.auto_tethering;

import android.app.Application;

import com.labs.dm.auto_tethering.db.DBManager;

/**
 * Created by daniel on 2015-11-07.
 */
public class MainApplication extends Application {

    private static DBManager db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = DBManager.getInstance(getApplicationContext());
    }

    public static DBManager getDBManager() {
        return db;
    }
}
