package com.labs.dm.auto_tethering.db;

/**
 * Created by daniel on 2015-11-07.
 */
public class SimCard {

    public final static String NAME = "SIMCARD";

    private int id;
    private String serial;
    private String number;
    private String imei;
    private int status;

    public SimCard(String serial, String number, String imei, int status) {
        this.serial = serial;
        this.number = number;
        this.imei = imei;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getSerial() {
        return serial;
    }

    public String getNumber() {
        return number;
    }

    public String getImei() {
        return imei;
    }

    public int getStatus() {
        return status;
    }
}
