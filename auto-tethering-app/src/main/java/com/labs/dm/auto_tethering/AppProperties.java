package com.labs.dm.auto_tethering;

/**
 * Created by Daniel Mroczka
 */
public class AppProperties {

    /**
     * Maximal number of configured Bluetooth Devices
     */
    public static final int MAX_BT_DEVICES = 5;
    /**
     * Maximal number of cellular networks
     */
    public static final int MAX_CELLULAR_ITEMS = 20;

    public static final int MAX_CELL_GROUPS_COUNT = 3;

    public static final String DEFAULT_IDLE_TETHERING_OFF_TIME = "60";

    public static final int TEMPERATURE_LIMIT = 40;

    public static final int MAX_SCHEDULED_ITEMS = 10;

    public static final int GPS_ACCURACY_LIMIT = 10;

    /**
     * Bluetooth timer
     */
    public static final int BT_TIMER_INTERVAL = 30000;

    public static final int BT_TIMER_START_DELAY = 5000;

    /**
     * Data usage timer
     */
    public static final int DATAUSAGE_TIMER_INTERVAL = 10000;

    public static final int DATAUSAGE_TIMER_START_DELAY = 1000;

    /**
     * Time when GPS request update has been switched off
     */
    public static final int GPS_UPDATE_TIMEOUT = 5000; //ms

    public static final String ACTIVATE_ON_STARTUP = "activate.on.startup";
    public static final String ACTIVATE_KEEP_SERVICE = "activate.keep.service";
    public static final String ACTIVATE_3G = "activate.3g";
    public static final String ACTIVATE_TETHERING = "activate.tethering";
    public static final String ACTIVATE_ON_SIMCARD = "activate.simcard";
    public static final String LATEST_VERSION = "latest.version";
    public static final String SSID = "ssid";
    public static final String IDLE_3G_OFF = "idle.3g.off";
    public static final String IDLE_3G_OFF_TIME = "idle.3g.off.time";
    public static final String IDLE_TETHERING_OFF = "idle.wifi.off";
    public static final String IDLE_TETHERING_OFF_TIME = "idle.wifi.off.time";
    public static final String ACTIVATE_ON_ROAMING = "activate.on.roaming";
    public static final String ACTIVATE_ON_ROAMING_HC = "activate.on.roaming.home.country";
    public static final String RETURN_TO_PREV_STATE = "return.state";

}
