package com.labs.dm.auto_tethering.service;

/**
 * Created by daniel on 2016-12-21.
 */

public enum Status {
    NONE,
    DEFAULT,
    DEACTIVATED_ON_IDLE,
    ACTIVATED_ON_SCHEDULE,
    DEACTIVATED_ON_SCHEDULE,
    USB_ON,
    DATA_USAGE_LIMIT_EXCEED,
    ACTIVATED_ON_CELL, DEACTIVATED_ON_CELL,
    TEMPERATURE_OFF,
    BT
}

