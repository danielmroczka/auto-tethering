package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel Mroczka on 10/6/2016.
 */
public class Bluetooth {
    public final static String NAME = "BLUETOOTH";

    private int id;
    private String name;
    private String address;
    private long used;

    public int getId() {
        return id;
    }

    private int status;

    public Bluetooth(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }
}
