package com.labs.dm.auto_tethering;

/**
 * Created by Daniel Mroczka on 6/8/2016.
 */
public class TetherIntents {
    //Invents registered in TetherService
    public static final String EXIT = "com.labs.dm.auto_tethering.EXIT";
    public static final String RESUME = "com.labs.dm.auto_tethering.RESUME";
    public static final String TETHERING = "tethering";
    public static final String WIDGET = "widget";
    public static final String USB_ON = "usb_on";
    public static final String USB_OFF = "usb_off";
    public static final String BT_RESTORE = "bt_set_idle";
    public static final String BT_CONNECTED = "bt.connected";
    public static final String BT_DISCONNECTED = "bt.disconnected";
    public static final String BT_SEARCH = "bt.search";
    public static final String TEMP_OVER = "temp.overheat.start";
    public static final String TEMP_BELOW = "temp.overheat.stop";

    //Invents registered in MainActivity
    public static final String CLIENTS = "clients";
    public static final String DATA_USAGE = "data.usage";
    public static final String UNLOCK = "unlock";
}
