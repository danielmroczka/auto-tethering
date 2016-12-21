package com.labs.dm.auto_tethering.service;

import com.labs.dm.auto_tethering.R;

/**
 * Created by Daniel Mroczka on 5/6/2016.
 */
public enum ServiceAction {

    TETHER_ON,
    TETHER_OFF(R.string.notification_tethering_off),
    INTERNET_ON,
    INTERNET_OFF,
    SCHEDULED_TETHER_ON,
    SCHEDULED_TETHER_OFF(R.string.notification_scheduled_tethering_off, Status.DEACTIVATED_ON_SCHEDULE),
    SCHEDULED_INTERNET_ON,
    SCHEDULED_INTERNET_OFF,
    TETHER_OFF_IDLE,
    INTERNET_OFF_IDLE,
    DATA_USAGE_EXCEED_LIMIT_INTERNET_TETHERING_OFF(R.string.notification_data_exceed_limit, Status.DATA_USAGE_LIMIT_EXCEED),
    ROAMING_INTERNET_TETHERING_OFF,
    SIMCARD_BLOCK_INTERNET_TETHERING_OFF,
    BLUETOOTH_INTERNET_TETHERING_ON(R.string.bluetooth_on, Status.BT),
    BLUETOOTH_INTERNET_TETHERING_OFF,
    CELL_INTERNET_TETHERING_ON(R.string.cell_on, Status.ACTIVATED_ON_CELL),
    CELL_INTERNET_TETHERING_OFF(R.string.cell_off, Status.DEACTIVATED_ON_CELL),
    TEMP_TETHERING_OFF(R.string.temp_off, Status.TEMPERATURE_OFF),
    TEMP_TETHERING_ON(R.string.temp_on, Status.DEFAULT);

    private final boolean on;
    private final boolean tethering;
    private final boolean internet;
    private int resource;
    private Status status;

    ServiceAction() {
        this.on = name().contains("ON");
        this.internet = name().contains("INTERNET");
        this.tethering = name().contains("TETHER");
    }

    ServiceAction(int resource) {
        this();
        this.resource = resource;
    }

    ServiceAction(int resource, Status status) {
        this(resource);
        this.status = status;
    }

    ServiceAction(boolean tethering, boolean internet, boolean on) {
        this.tethering = tethering;
        this.internet = internet;
        this.on = on;
    }

    ServiceAction(boolean tethering, boolean internet, boolean on, int resource) {
        this(tethering, internet, on);
        this.resource = resource;
    }

    public boolean isOn() {
        return on;
    }

    public boolean isTethering() {
        return tethering;
    }

    public boolean isInternet() {
        return internet;
    }

    public int getResource() {
        return resource;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return name() + ", isTethering=" + isTethering() + ", isInternet=" + isInternet() + ", state=" + isOn();
    }
}
