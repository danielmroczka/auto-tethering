package com.labs.dm.auto_tethering;

/**
 * Created by Daniel Mroczka on 9/12/2016.
 */

public class Loc {

    public int getLac() {
        return lac;
    }

    public int getCid() {
        return cid;
    }

    private int cid;
    private int lac;

    public Loc(int cid, int lac) {
        this.cid = cid;
        this.lac = lac;
    }

    public Loc(String loc) {
        if (loc != null && !loc.isEmpty()) {
            cid = Integer.parseInt(loc.substring(loc.indexOf("CID:") + 5, loc.indexOf(" LOC:")));
            lac = Integer.parseInt(loc.substring(loc.indexOf("LOC:") + 5, loc.length()));
        }
    }

    @Override
    public String toString() {
        return "CID: " + String.valueOf(cid) + " LOC: " + String.valueOf(lac);
    }

    public String getLoc() {
        if (isValid()) {
            return toString();
        } else return "";
    }

    public boolean isValid() {
        return (cid > 0 && lac > 0);
    }
}