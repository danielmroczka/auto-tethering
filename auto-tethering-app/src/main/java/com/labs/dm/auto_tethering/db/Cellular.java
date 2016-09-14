package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel Mroczka on 2016-09-13.
 */

public class Cellular {
    public final static String NAME = "CELLULAR";

    private int id;
    private final int mcc;
    private final int mnc;
    private final int lac;
    private final int cid;
    private final char type;
    private final float lat;
    private final float lon;
    private final String name;
    private int status;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getMcc() {
        return mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public int getLac() {
        return lac;
    }

    public int getCid() {
        return cid;
    }

    public char getType() {
        return type;
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public Cellular(int mcc, int mnc, int lac, int cid, char type, float lat, float lon, String name, int status) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.lac = lac;
        this.cid = cid;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.status = status;
    }
}
