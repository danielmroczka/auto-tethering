package com.labs.dm.auto_tethering;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by daniel on 2015-09-29.
 */
public class AppProperties {

    private static final String ACTIVATE_ON_STARTUP = "activate.on.startup";
    private static final String ACTIVATE_3G = "activate.3g";
    private static final String ACTIVATE_TETHERING = "activate.tethering";
    private static final String SCHEDULER = "scheduler.on";
    private static final String TIME_OFF = "time.off";
    private static final String TIME_ON = "time.on";
    private static final String ACTIVATE_ON_PHONE_NUMBER = "activate.only.on.phone.number";
    private static final String PHONE_NUMBER = "activated.phone.number";
    private static final String SIMCARD = "sim.card";
    private static final String LATEST_VERSION = "latest.version";

    private SharedPreferences sp;
    private boolean activateOnStartup;
    private boolean activate3G;
    private boolean activateTethering;
    private boolean scheduler;
    private boolean activateOnPhoneNumber;
    private String timeOff;
    private String timeOn;
    private String phoneNumber;
    private String simCard;
    private String latestVersion;

    public boolean isScheduler() {
        return scheduler;
    }

    public void setScheduler(boolean scheduler) {
        this.scheduler = scheduler;
    }

    public void load(Context ctx) {
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        activateOnStartup = sp.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, true);
        activate3G = sp.getBoolean(AppProperties.ACTIVATE_3G, false);
        activateTethering = sp.getBoolean(AppProperties.ACTIVATE_TETHERING, true);
        scheduler = sp.getBoolean(AppProperties.SCHEDULER, false);
        timeOff = sp.getString(AppProperties.TIME_OFF, "0:00");
        timeOn = sp.getString(AppProperties.TIME_ON, "6:00");
        simCard = sp.getString(AppProperties.SIMCARD, "");
        latestVersion = sp.getString(AppProperties.LATEST_VERSION, "");
    }

    public void save(Context ctx) {
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ACTIVATE_ON_STARTUP, activateOnStartup);
        editor.putBoolean(ACTIVATE_3G, activate3G);
        editor.putBoolean(ACTIVATE_TETHERING, activateTethering);
        editor.putBoolean(SCHEDULER, scheduler);
        editor.putString(TIME_OFF, timeOff);
        editor.putString(TIME_ON, timeOn);
        editor.putString(SIMCARD, simCard);
        editor.putString(LATEST_VERSION, latestVersion);
        editor.apply();
    }

    public boolean isActivateOnStartup() {
        return activateOnStartup;
    }

    public void setActivateOnStartup(boolean activateOnStartup) {
        this.activateOnStartup = activateOnStartup;
    }

    public boolean isActivate3G() {
        return activate3G;
    }

    public void setActivate3G(boolean activate3G) {
        this.activate3G = activate3G;
    }

    public boolean isActivateTethering() {
        return activateTethering;
    }

    public void setActivateTethering(boolean activateTethering) {
        this.activateTethering = activateTethering;
    }

    public String getTimeOff() {
        return timeOff;
    }

    public void setTimeOff(String timeOff) {
        this.timeOff = timeOff;
    }

    public String getTimeOn() {
        return timeOn;
    }

    public void setTimeOn(String timeOn) {
        this.timeOn = timeOn;
    }

    public boolean isActivateOnPhoneNumber() {
        return activateOnPhoneNumber;
    }

    public void setActivateOnPhoneNumber(boolean activateOnPhoneNumber) {
        this.activateOnPhoneNumber = activateOnPhoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSimCard() {
        return simCard;
    }

    public void setSimCard(String simCard) {
        this.simCard = simCard;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }
}
