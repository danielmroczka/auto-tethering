package com.labs.dm.auto_tethering.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 2015-07-06.
 */
public class DBManager extends SQLiteOpenHelper {

    private final SQLiteDatabase writableDatabase;
    private final SQLiteDatabase readableDatabase;
    public final static String DB_NAME = "autowifi.db";

    private static DBManager instance;

    public static DBManager getInstance(Context context) {
        if (instance == null) {
            instance = new DBManager(context);
        }
        return instance;
    }

    private DBManager(Context context, String name) {
        super(context, name, null, 1);
        writableDatabase = getWritableDatabase();
        readableDatabase = getReadableDatabase();
    }

    private DBManager(Context context) {
        this(context, DB_NAME);
    }


    @Override
    public synchronized void close() {
        writableDatabase.close();
        readableDatabase.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // CREATE TABLE
        db.execSQL("create table SIMCARD(id INTEGER PRIMARY KEY, serial VARCHAR(20), number VARCHAR(20), imei VARCHAR(20), status INTEGER)");
        // CREATE INDEX
        db.execSQL("create unique index SIMCARD_UNIQUE_IDX on simcard(serial, number, imei)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<SimCard> readSimCard() {
        List<SimCard> list;
        Cursor cursor = null;
        try {
            cursor = writableDatabase.rawQuery("SELECT id, serial, number, imei, status FROM SIMCARD", null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    SimCard p = new SimCard(cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4));
                    list.add(p);
                }
                while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public long addSimCard(SimCard simCard) {
        ContentValues content = new ContentValues();
        content.put("serial", simCard.getSerial());
        content.put("number", simCard.getNumber());
        content.put("imei", simCard.getImei());
        content.put("status", simCard.getStatus());
        return writableDatabase.insert(SimCard.NAME, null, content);
    }

    public boolean isOnWhiteList(final String ssn) {
        boolean res;
        Cursor cursor = null;
        try {
            cursor = writableDatabase.rawQuery("SELECT 1 FROM SIMCARD where serial = '" + ssn + "'", null);
            res = cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }


    public int removeSimCard(final String ssn) {
        //writableDatabase.execSQL("delete from SIMCARD where serial='" + ssn + "'" );

        return writableDatabase.delete(SimCard.NAME, "serial='" + ssn + "'", null);
    }
}
