package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel Mroczka on 2016-09-27.
 */
public class CellGroup {
    public final static String NAME = "CELL_GROUP";
    private int id, status;
    private final String name;
    private final String type;

    public CellGroup(String name, String type, int status) {
        this.type = type;
        this.status = status;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public enum STATUS {
        DISABLED(0), ENABLED(1);
        final int value;

        STATUS(int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }
    }
}
