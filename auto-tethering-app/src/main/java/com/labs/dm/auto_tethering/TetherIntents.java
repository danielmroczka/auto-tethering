package com.labs.dm.auto_tethering;

/**
 * Created by Daniel Mroczka on 6/8/2016.
 */
public class TetherIntents {
    //Invents registered in TetherService
    public static final String EXIT = "com.labs.dm.auto_tethering.EXIT";
    public static final String RESUME = "com.labs.dm.auto_tethering.RESUME";
    public static final String TETHERING = "tethering";
    public static final String SERVICE_ON = "service.on";
    public static final String WIDGET = "widget";
    public static final String USB_ON = "usb_on";
    public static final String USB_OFF = "usb_off";
    public static final String BT_RESTORE = "bt_set_idle";
    public static final String BT_CONNECTED = "bt.connected";
    public static final String BT_DISCONNECTED = "bt.disconnected";
    public static final String BT_SEARCH = "bt.search";
    public static final String TEMPERATURE_ABOVE_LIMIT = "temp.overheat.start";
    public static final String TEMPERATURE_BELOW_LIMIT = "temp.overheat.stop";
    public static final String CHANGE_NETWORK_STATE = "change.net.state";
    public static final String CHANGE_CELL = "change.cell";

    public static final String TETHER_ON = "tether.on";
    public static final String TETHER_OFF = "tether.off";
    public static final String INTERNET_ON = "internet.on";
    public static final String INTERNET_OFF = "internet.off";

    //Invents registered in MainActivity
    public static final String CLIENTS = "clients";
    public static final String DATA_USAGE = "data.usage";
    public static final String UNLOCK = "unlock";
    public static final String EVENT_WIFI_ON = "wifi.on";
    public static final String EVENT_WIFI_OFF = "wifi.off";
    public static final String EVENT_MOBILE_ON = "mobile.on";
    public static final String EVENT_MOBILE_OFF = "mobile.off";
    public static final String EVENT_TETHER_ON = "on.tether.on";
    public static final String EVENT_TETHER_OFF = "on.tether.off";
}
