package com.labs.dm.auto_tethering.db;

/**
 * Created by daniel on 2015-11-18.
 */
public class Cron {

    public final static String NAME = "CRON";

    public enum STATUS {
        ACTIVE(1), OFF(0);

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
