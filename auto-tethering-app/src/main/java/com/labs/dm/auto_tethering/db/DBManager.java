package com.labs.dm.auto_tethering.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Daniel Mroczka on 2015-07-06.
 */
public class DBManager extends SQLiteOpenHelper {

    private final SQLiteDatabase writableDatabase;
    private final SQLiteDatabase readableDatabase;
    public final static String DB_NAME = "autowifi.db";
    private static final int DB_VERSION = 5;

    private static DBManager instance;

    public static synchronized DBManager getInstance(Context context) {
        if (instance == null) {
            instance = new DBManager(context.getApplicationContext());
        }
        return instance;
    }

    private DBManager(Context context, String name) {
        super(context, name, null, DB_VERSION);
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
        db.execSQL("create table SIMCARD(id INTEGER PRIMARY KEY, ssn VARCHAR(20), number VARCHAR(20), status INTEGER)");
        db.execSQL("create table CRON(id INTEGER PRIMARY KEY, hourOff INTEGER, minOff INTEGER, hourOn INTEGER, minOn INTEGER, mask INTEGER, status INTEGER)");
        // CREATE INDEX
        db.execSQL("create unique index SIMCARD_UNIQUE_IDX on simcard(ssn, number)");
        db.execSQL("create unique index CRON_UNIQUE_IDX on cron(hourOff ,minOff , hourOn, minOn, mask)");
        Log.i("DBManager", "DB structure created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DBManager", "onUpgrade old=" + oldVersion + ", new=" + newVersion);
        if (oldVersion < 4) {
            db.execSQL("drop table IF EXISTS CRON");
            db.execSQL("create table CRON(id INTEGER PRIMARY KEY, hourOff INTEGER, minOff INTEGER, hourOn INTEGER, minOn INTEGER, mask INTEGER, status INTEGER)");
            db.execSQL("create unique index CRON_UNIQUE_IDX on cron(hourOff ,minOff , hourOn, minOn, mask)");
        } else if (oldVersion < 5) {
            db.execSQL("drop table IF EXISTS CELLULAR");
            db.execSQL("create table CELLULAR(id INTEGER PRIMARY KEY, mcc INTEGER, mnc INTEGER, lac INTEGER, cid INTEGER, type TEXT, lat REAL, lon REAL, name TEXT, status INTEGER)");
        }
        Log.i("DBManager", "DB upgraded from version " + oldVersion + " to " + newVersion);
    }

    public List<SimCard> readSimCard() {
        List<SimCard> list;
        Cursor cursor = null;
        try {
            cursor = readableDatabase.rawQuery("SELECT id, ssn, number, status FROM SIMCARD", null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    SimCard p = new SimCard(cursor.getString(1), cursor.getString(2), cursor.getInt(3));
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
        content.put("ssn", simCard.getSsn());
        content.put("number", simCard.getNumber());
        content.put("status", simCard.getStatus());
        return writableDatabase.insert(SimCard.NAME, null, content);
    }

    public boolean isOnWhiteList(final String ssn) {
        boolean res;
        Cursor cursor = null;
        try {
            cursor = writableDatabase.rawQuery("SELECT 1 FROM SIMCARD where ssn = '" + ssn + "'", null);
            res = cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }

    public int removeSimCard(final String ssn) {
        return writableDatabase.delete(SimCard.NAME, "ssn='" + ssn + "'", null);
    }

    public List<Cron> getCrons() {
        List<Cron> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = readableDatabase.query(Cron.NAME, null, null, null, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Cron cron = new Cron(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6));
                    cron.setId(cursor.getInt(0));
                    list.add(cron);
                }
                while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Collections.sort(list, new Comparator<Cron>() {
            @Override
            public int compare(Cron lhs, Cron rhs) {
                int diffOff = 60 * (lhs.getHourOff() - rhs.getHourOff()) + (lhs.getMinOff() - rhs.getMinOff());
                int diffOn = 60 * (lhs.getHourOn() - rhs.getHourOn()) + (lhs.getMinOn() - rhs.getMinOn());
                return diffOff > 0 ? diffOff : diffOn;
            }
        });
        return list;
    }

    public Cron getCron(int id) {
        Cursor cursor = null;
        Cron cron = null;
        try {
            cursor = readableDatabase.query(Cron.NAME, null, "id=" + id, null, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                cron = new Cron(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6));
                cron.setId(cursor.getInt(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return cron;
    }

    public int removeCron(final int id) {
        return writableDatabase.delete(Cron.NAME, "id=" + String.valueOf(id), null);
    }

    public long addOrUpdateCron(SQLiteDatabase db, Cron cron) {
        ContentValues content = new ContentValues();
        content.put("hourOff", cron.getHourOff());
        content.put("minOff", cron.getMinOff());
        content.put("hourOn", cron.getHourOn());
        content.put("minOn", cron.getMinOn());
        content.put("mask", cron.getMask());
        content.put("status", cron.getStatus());

        if (cron.getId() > 0) {
            return db.update(Cron.NAME, content, "id=" + cron.getId(), null);
        } else {
            return db.insert(Cron.NAME, null, content);
        }
    }

    public long addOrUpdateCron(Cron cron) {
        return addOrUpdateCron(writableDatabase, cron);
    }

    public void reset() {
        getWritableDatabase().delete(SimCard.NAME, null, null);
        getWritableDatabase().delete(Cron.NAME, null, null);
    }

    public long addCellular(Cellular cellular) {
        ContentValues content = new ContentValues();
        content.put("cid", cellular.getCid());
        content.put("lac", cellular.getLac());
        content.put("mcc", cellular.getMcc());
        content.put("mnc", cellular.getMnc());

        content.put("id", cellular.getId());
        content.put("type", String.valueOf(cellular.getType()));
        content.put("lat", cellular.getLat());
        content.put("lon", cellular.getLon());
        content.put("name", cellular.getName());
        content.put("status", cellular.getStatus());
        return writableDatabase.insert(Cellular.NAME, null, content);
    }

    public List<Cellular> readCellular(char type) {
        List<Cellular> list;
        Cursor cursor = null;
        try {
            cursor = readableDatabase.rawQuery("SELECT id, mcc, mnc, lac, cid, type, lat, lon, name, status FROM CELLULAR where type='" + type + "'", null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Cellular p = new Cellular(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), (char) cursor.getInt(5), cursor.getDouble(6), cursor.getDouble(7), cursor.getString(8), cursor.getInt(9));
                    p.setId(cursor.getInt(0));
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

    public int removeCellular(final String id) {
        return writableDatabase.delete(Cellular.NAME, "id=" + Integer.valueOf(id), null);
    }
}
