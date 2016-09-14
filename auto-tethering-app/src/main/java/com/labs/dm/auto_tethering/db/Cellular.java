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
    private char type;
    private double lat;
    private double lon;
    private String name;
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

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public Cellular(int mcc, int mnc, int lac, int cid) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.lac = lac;
        this.cid = cid;
    }

    public Cellular(int mcc, int mnc, int lac, int cid, char type, double lat, double lon, String name, int status) {
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

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public boolean theSame(Cellular other) {
        return other != null && other.getLat() == getLac() && other.getLon() == getLon() && other.getMcc() == getMcc() && other.getMnc() == getMnc();
    }

    public void setType(char type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }
}
