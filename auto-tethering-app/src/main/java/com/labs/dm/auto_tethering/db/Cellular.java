package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel Mroczka on 2016-09-13.
 */

public class Cellular {
    public final static String NAME = "CELLULAR";

    private int id, mcc, mnc, lac, cid, status, cellGroup;
    private double lat, lon;

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

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getStatus() {
        return status;
    }

    public Cellular() {
    }

    public Cellular(int mcc, int mnc, int lac, int cid) {
        this();
        this.mcc = mcc;
        this.mnc = mnc;
        this.lac = lac;
        this.cid = cid;
    }

    public Cellular(int mcc, int mnc, int lac, int cid, double lat, double lon, int status, int cellgroup) {
        this(mcc, mnc, lac, cid);
        this.lat = lat;
        this.lon = lon;
        this.status = status;
        this.cellGroup = cellgroup;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null && !(object instanceof Cellular)) {
            return false;
        }
        Cellular other = (Cellular) object;
        return other != null && other.getLac() == getLac() && other.getCid() == getCid() && other.getMcc() == getMcc() && other.getMnc() == getMnc();
    }

    @Override
    public String toString() {
        return cid + "/" + lac;
    }

    public boolean hasLocation() {
        return lon != 0 && lat != 0;
    }

    public boolean isValid() {
        return cid > 0 && lac > 0 && mnc > 0 && mcc > 0;
    }

    public int getCellGroup() {
        return cellGroup;
    }

    public void setCellGroup(int cellGroup) {
        this.cellGroup = cellGroup;
    }

}
