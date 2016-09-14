package com.labs.dm.auto_tethering;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */

public class CellInfo {

    private int cid;
    private int lac;
    private int mcc;
    private int mnc;

    public CellInfo(int cid, int lac, int mcc, int mnc) {
        this.cid = cid;
        this.lac = lac;
        this.mcc = mcc;
        this.mnc = mnc;
    }

    public CellInfo(int cid, int lac) {
        this.cid = cid;
        this.lac = lac;
    }

    public int getLac() {
        return lac;
    }

    public int getCid() {
        return cid;
    }

    public int getMcc() {
        return mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public CellInfo(String loc) {
        if (loc != null && !loc.isEmpty()) {
            cid = Integer.parseInt(loc.substring(loc.indexOf("CID:") + 5, loc.indexOf(" LAC:")));
            lac = Integer.parseInt(loc.substring(loc.indexOf("LAC:") + 5, loc.length()));
        }
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "CID: " + String.valueOf(cid) + " LAC: " + String.valueOf(lac);
        } else {
            return "";
        }
    }

    public boolean isValid() {
        return (cid > 0 && lac > 0);
    }
}