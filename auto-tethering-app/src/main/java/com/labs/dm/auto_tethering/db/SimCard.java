package com.labs.dm.auto_tethering.db;

/**
 * Created by daniel on 2015-11-07.
 */
public class SimCard {

    public final static String NAME = "SIMCARD";

    private int id;
    private String ssn;
    private String number;
    private int status;

    public SimCard(String ssn, String number, int status) {
        this.ssn = ssn;
        this.number = number;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getSsn() {
        return ssn;
    }

    public String getNumber() {
        return number;
    }

    public int getStatus() {
        return status;
    }
}
