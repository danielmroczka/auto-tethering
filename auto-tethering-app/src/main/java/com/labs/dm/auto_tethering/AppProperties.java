package com.labs.dm.auto_tethering;

/**
 * Created by Daniel Mroczka
 */
public class AppProperties {

    public static final int MAX_BT_DEVICES = 3;

    public static final String ACTIVATE_ON_STARTUP = "activate.on.startup";
    public static final String ACTIVATE_KEEP_SERVICE = "activate.keep.service";
    public static final String ACTIVATE_3G = "activate.3g";
    public static final String ACTIVATE_TETHERING = "activate.tethering";
    public static final String SCHEDULER = "scheduler.on";
    public static final String TIME_OFF = "scheduler.time.off";
    public static final String TIME_ON = "scheduler.time.on";
    public static final String ACTIVATE_ON_SIMCARD = "activate.simcard";
    public static final String SIMCARD_LIST = "simcard.list";
    public static final String LATEST_VERSION = "latest.version";
    public static final String SSID = "ssid";
    public static final String IDLE_3G_OFF = "idle.3g.off";
    public static final String IDLE_3G_OFF_TIME = "idle.3g.off.time";
    public static final String IDLE_TETHERING_OFF = "idle.wifi.off";
    public static final String IDLE_TETHERING_OFF_TIME = "idle.wifi.off.time";
    public static final String ACTIVATE_ON_ROAMING = "activate.on.roaming";
    public static final String RETURN_TO_PREV_STATE = "return.state";
    public static final String FORCE_NET_FROM_NOTIFY = "force.net.from.notify";

    //DEFAULT
    public static final String DEFAULT_IDLE_TETHERING_OFF_TIME = "60";
}
