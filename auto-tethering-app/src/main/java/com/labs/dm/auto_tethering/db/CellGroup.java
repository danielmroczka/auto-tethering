package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel mroczka on 2016-09-27.
 */
public class CellGroup {
    public final static String NAME = "CELL_GROUP";
    private int id;
    private int status;
    private String name;
    private String type;

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

    public void toggle() {
        if (getStatus() == STATUS.ENABLED.getValue()) {
            status = STATUS.DISABLED.getValue();
        } else if (getStatus() == STATUS.DISABLED.getValue()) {
            status = STATUS.ENABLED.getValue();
        }
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
