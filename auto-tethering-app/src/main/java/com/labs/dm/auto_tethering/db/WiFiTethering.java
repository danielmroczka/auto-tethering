package com.labs.dm.auto_tethering.db;

/**
 * Created by Daniel Mroczka on 21-Mar-17.
 */

public class WiFiTethering {
    public final static String NAME = "WIFI_TETHERING";

    private int id;
    private String ssid;
    private SECURITY_TYPE type;
    private String password;
    private int channel;
    private int status;
    private boolean defaultWiFi;

    public WiFiTethering(String ssid, SECURITY_TYPE type, String password, int channel, int status) {
        this.ssid = ssid;
        this.type = type;
        this.password = password;
        this.channel = channel;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public SECURITY_TYPE getType() {
        return type;
    }

    public void setType(SECURITY_TYPE type) {
        this.type = type;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isDefaultWiFi() {
        return defaultWiFi;
    }

    public void setDefaultWiFi(boolean defaultWiFi) {
        this.defaultWiFi = defaultWiFi;
    }

    public enum SECURITY_TYPE {

        OPEN("OPEN"), WPAPSK("WPAPSK"), WPA2PSK("WPA2PSK"), NONE(""), NULL(null);

        private final String name;

        SECURITY_TYPE(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

    }
}
