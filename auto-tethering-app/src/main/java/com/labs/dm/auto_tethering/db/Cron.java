package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel Mroczka on 2015-11-18.
 */
public class Cron {

    public final static String NAME = "CRON";

    public enum STATUS {
        DEFAULT(0), SCHED_OFF_ENABLED(1), SCHED_OFF_DISABLED(2), SCHED_ON_ENABLED(3), SCHED_ON_DISABLED(4);

        int value;

        STATUS(int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }

    }

    private int id;
    private int hourOff;
    private int minOff;
    private int hourOn;
    private int minOn;
    private int mask;
    private int status;

    public Cron(int hourOff, int minOff, int hourOn, int minOn, int mask, int status) {
        this.hourOff = hourOff;
        this.hourOn = hourOn;
        this.minOff = minOff;
        this.minOn = minOn;
        this.mask = mask;
        this.status = status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getHourOff() {
        return hourOff;
    }

    public int getHourOn() {
        return hourOn;
    }

    public int getMinOff() {
        return minOff;
    }

    public int getMinOn() {
        return minOn;
    }

    public int getMask() {
        return mask;
    }

    public int getStatus() {
        return status;
    }
}
