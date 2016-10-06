package com.labs.dm.auto_tethering.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import com.labs.dm.auto_tethering.MyLog;

import java.util.*;

/**
 * Created by Daniel Mroczka on 2015-07-06.
 */
public class DBManager extends SQLiteOpenHelper {

    public final static String DB_NAME = "autowifi.db";
    private static final int DB_VERSION = 6;
    private Context context;

    private static DBManager instance;

    public static synchronized DBManager getInstance(Context context) {
        if (instance == null) {
            instance = new DBManager(context);
        }
        return instance;
    }

    private DBManager(Context context, String name) {
        super(context, name, null, DB_VERSION);
        this.context = context;
    }

    private DBManager(Context context) {
        this(context, DB_NAME);
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // CREATE TABLE
        db.execSQL("create table SIMCARD(id INTEGER PRIMARY KEY, ssn VARCHAR(20), number VARCHAR(20), status INTEGER)");
        db.execSQL("create table CRON(id INTEGER PRIMARY KEY, hourOff INTEGER, minOff INTEGER, hourOn INTEGER, minOn INTEGER, mask INTEGER, status INTEGER)");
        db.execSQL("create table CELL_GROUP(id INTEGER PRIMARY KEY, name TEXT, type TEXT, status INTEGER)");
        db.execSQL("create table CELLULAR(id INTEGER PRIMARY KEY, mcc INTEGER, mnc INTEGER, lac INTEGER, cid INTEGER, lat REAL, lon REAL, cellgroup INTEGER, status INTEGER, FOREIGN KEY(cellgroup) REFERENCES CELL_GROUP(id) ON DELETE CASCADE)");
        db.execSQL("create table BLUETOOTH(id INTEGER PRIMARY KEY, name VARCHAR(40), address VARCHAR(20), used datetime, type, INTEGER, status INTEGER)");
        // CREATE INDEX
        db.execSQL("create unique index SIMCARD_UNIQUE_IDX on simcard(ssn, number)");
        db.execSQL("create unique index CRON_UNIQUE_IDX on cron(hourOff ,minOff , hourOn, minOn, mask)");
        db.execSQL("create unique index CELLULAR_UNIQUE_IDX on cellular(mcc, mnc, lac, cid, cellgroup)");
        db.execSQL("create unique index CELL_GROUP_UNIQUE_IDX on cell_group(name, type)");
        db.execSQL("create unique index BLUETOOTH_UNIQUE_IDX on bluetooth(name)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MyLog.i("DBManager", "onUpgrade old=" + oldVersion + ", new=" + newVersion);
        if (oldVersion < 4) {
            // DROP TABLE
            db.execSQL("drop table IF EXISTS CRON");
            // CREATE TABLE
            db.execSQL("create table CRON(id INTEGER PRIMARY KEY, hourOff INTEGER, minOff INTEGER, hourOn INTEGER, minOn INTEGER, mask INTEGER, status INTEGER)");
            // CREATE INDEX
            db.execSQL("create unique index CRON_UNIQUE_IDX on cron(hourOff ,minOff , hourOn, minOn, mask)");
        } else if (oldVersion < 5) {
            // DROP TABLE
            db.execSQL("drop table IF EXISTS CELLULAR");
            db.execSQL("drop table IF EXISTS CELL_GROUP");
            // CREATE TABLE
            db.execSQL("create table CELL_GROUP(id INTEGER PRIMARY KEY, name TEXT, type TEXT, status INTEGER)");
            db.execSQL("create table CELLULAR(id INTEGER PRIMARY KEY, mcc INTEGER, mnc INTEGER, lac INTEGER, cid INTEGER, lat REAL, lon REAL, cellgroup INTEGER, status INTEGER, FOREIGN KEY(cellgroup) REFERENCES CELL_GROUP(id) ON DELETE CASCADE)");
            // CREATE INDEX
            db.execSQL("create unique index CELLULAR_UNIQUE_IDX on cellular(mcc,mnc, lac, cid, cellgroup)");
            db.execSQL("create unique index CELL_GROUP_UNIQUE_IDX on cell_group(name, type)");
        } else if (oldVersion < 6) {
            // CREATE TABLE
            db.execSQL("drop table IF EXISTS BLUETOOTH");
            db.execSQL("create table BLUETOOTH(id INTEGER PRIMARY KEY, name VARCHAR(40), address VARCHAR(20), used datetime, type, INTEGER, status INTEGER)");
            // CREATE INDEX
            db.execSQL("create unique index BLUETOOTH_UNIQUE_IDX on bluetooth(name)");
            importBluetooth(db);
        }
        MyLog.i("DBManager", "DB upgraded from version " + oldVersion + " to " + newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    public List<SimCard> readSimCard() {
        List<SimCard> list;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT id, ssn, number, status FROM SIMCARD", null);
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
        return getWritableDatabase().insert(SimCard.NAME, null, content);
    }

    public boolean isOnWhiteList(final String ssn) {
        boolean res;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT 1 FROM SIMCARD where ssn = '" + ssn + "'", null);
            res = cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }

    public int removeSimCard(final String ssn) {
        return getWritableDatabase().delete(SimCard.NAME, "ssn='" + ssn + "'", null);
    }

    public List<Cron> getCrons() {
        List<Cron> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(Cron.NAME, null, null, null, null, null, null);
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
            cursor = getReadableDatabase().query(Cron.NAME, null, "id=" + id, null, null, null, null);
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
        return getWritableDatabase().delete(Cron.NAME, "id=" + String.valueOf(id), null);
    }

    public long addOrUpdateCron(Cron cron) {
        ContentValues content = new ContentValues();
        content.put("hourOff", cron.getHourOff());
        content.put("minOff", cron.getMinOff());
        content.put("hourOn", cron.getHourOn());
        content.put("minOn", cron.getMinOn());
        content.put("mask", cron.getMask());
        content.put("status", cron.getStatus());
        return addOrUpdate(cron.getId(), Cron.NAME, content);
    }

    public void removeAllData() {
        getWritableDatabase().delete(SimCard.NAME, null, null);
        getWritableDatabase().delete(Cron.NAME, null, null);
        getWritableDatabase().delete(Cellular.NAME, null, null);
        getWritableDatabase().delete(CellGroup.NAME, null, null);
    }

    public long addOrUpdateCellular(Cellular cellular) {
        ContentValues content = new ContentValues();
        content.put("cid", cellular.getCid());
        content.put("lac", cellular.getLac());
        content.put("mcc", cellular.getMcc());
        content.put("mnc", cellular.getMnc());
        content.put("cellgroup", cellular.getCellGroup());
        content.put("lat", cellular.getLat());
        content.put("lon", cellular.getLon());
        content.put("status", cellular.getStatus());
        return addOrUpdate(cellular.getId(), Cellular.NAME, content);
    }

    public List<Cellular> readCellular(int groupId) {
        List<Cellular> list;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT id, mcc, mnc, lac, cid, lat, lon, status, cellgroup FROM CELLULAR where cellgroup=" + groupId, null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Cellular p = new Cellular(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getInt(7), cursor.getInt(8));
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
        return getWritableDatabase().delete(Cellular.NAME, "id=" + Integer.valueOf(id), null);
    }

    public long addOrUpdateCellGroup(CellGroup cellGroup) {
        ContentValues content = new ContentValues();
        content.put("status", cellGroup.getStatus());
        content.put("name", cellGroup.getName());
        content.put("type", cellGroup.getType());
        return addOrUpdate(cellGroup.getId(), CellGroup.NAME, content);
    }

    public int removeCellGroup(int id) {
        return getWritableDatabase().delete(CellGroup.NAME, "id=" + Integer.valueOf(id), null);
    }

    public List<CellGroup> loadCellGroup(String type) {
        List<CellGroup> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(CellGroup.NAME, null, "type=?", new String[]{type}, "type, name", null, "status desc, name");
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    CellGroup cellGroup = new CellGroup(cursor.getString(1), cursor.getString(2), cursor.getInt(3));
                    cellGroup.setId(cursor.getInt(0));
                    list.add(cellGroup);
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

    private long addOrUpdate(int id, String name, ContentValues content) {
        if (id > 0) {
            return getWritableDatabase().update(name, content, "id=" + id, null);
        } else {
            return getWritableDatabase().insert(name, null, content);
        }
    }

    public int toggleCellGroup(CellGroup group) {
        ContentValues content = new ContentValues();
        content.put("status", group.getStatus() == CellGroup.STATUS.ENABLED.getValue() ? CellGroup.STATUS.DISABLED.getValue() : CellGroup.STATUS.ENABLED.getValue());
        return getWritableDatabase().update(CellGroup.NAME, content, "id=" + group.getId(), null);
    }

    public List<Cellular> readAllCellular(String type) {
        List<Cellular> list;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT c.id, c.mcc, c.mnc, c.lac, c.cid, c.lat, c.lon, c.status, c.cellgroup FROM CELLULAR c, CELL_GROUP g where c.cellgroup = g.id and g.type='" + type + "'", null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Cellular p = new Cellular(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getInt(7), cursor.getInt(8));
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

    public List<Cellular> readCellular(String type) {
        List<Cellular> list;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT c.id, c.mcc, c.mnc, c.lac, c.cid, c.lat, c.lon, c.status, c.cellgroup FROM CELLULAR c, CELL_GROUP g where c.cellgroup = g.id and g.status = " + CellGroup.STATUS.ENABLED.getValue() + " and g.type='" + type + "'", null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Cellular p = new Cellular(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getInt(7), cursor.getInt(8));
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

    public List<Bluetooth> readBluetooth() {
        List<Bluetooth> list;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT id, name, address FROM BLUETOOTH order by used", null);
            list = new ArrayList<>(cursor.getCount());
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Bluetooth p = new Bluetooth(cursor.getString(1), cursor.getString(2));
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

    public int removeBluetooth(int id) {
        return getWritableDatabase().delete(Bluetooth.NAME, "id=" + id, null);
    }

    public long addOrUpdateBluetooth(Bluetooth bluetooth) {
        ContentValues content = new ContentValues();
        content.put("status", bluetooth.getStatus());
        content.put("name", bluetooth.getName());
        content.put("address", bluetooth.getAddress());
        content.put("used", bluetooth.getUsed());
        return addOrUpdate(bluetooth.getId(), Bluetooth.NAME, content);
    }

    private void importBluetooth(SQLiteDatabase db) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> map = prefs.getAll();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().startsWith("bt.devices.")) {
                Bluetooth bluetooth = new Bluetooth((String) entry.getValue(), "");
                ContentValues content = new ContentValues();
                content.put("name", bluetooth.getName());
                db.insert(Bluetooth.NAME, null, content);
                prefs.edit().remove(entry.getKey()).commit();
            }
            if (entry.getKey().startsWith("bt.last.connect")) {
                prefs.edit().remove(entry.getKey()).commit();
            }
        }
    }
}
