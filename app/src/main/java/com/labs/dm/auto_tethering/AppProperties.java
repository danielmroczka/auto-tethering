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
    private final SharedPreferences sp;
    private boolean activateOnStartup;
    private boolean activate3G;
    private boolean activateTethering;

    public AppProperties(Context ctx) {
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        load();
    }

    private void load() {
        activateOnStartup = sp.getBoolean(AppProperties.ACTIVATE_ON_STARTUP, true);
        activate3G = sp.getBoolean(AppProperties.ACTIVATE_3G, true);
        activateTethering = sp.getBoolean(AppProperties.ACTIVATE_TETHERING, true);
    }

    public void save() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ACTIVATE_ON_STARTUP, activateOnStartup);
        editor.putBoolean(ACTIVATE_3G, activate3G);
        editor.putBoolean(ACTIVATE_TETHERING, activateTethering);
        editor.commit();
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
}
