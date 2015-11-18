package com.labs.dm.auto_tethering.db;

/**
 * Created by daniel on 2015-11-18.
 */
public class Cron {

    public final static String NAME = "CRON";

    private int id;
    private String timeOff;
    private String timeOn;
    private int mask;
    private int status;

    public Cron(int id, String timeOff, String timeOn, int mask, int status) {
        this.id = id;
        this.timeOff = timeOff;
        this.timeOn = timeOn;
        this.mask = mask;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getTimeOff() {
        return timeOff;
    }

    public String getTimeOn() {
        return timeOn;
    }

    public int getMask() {
        return mask;
    }

    public int getStatus() {
        return status;
    }
}
