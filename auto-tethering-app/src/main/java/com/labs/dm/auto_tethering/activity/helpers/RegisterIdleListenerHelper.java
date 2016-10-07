package com.labs.dm.auto_tethering.activity.helpers;

import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import com.labs.dm.auto_tethering.Utils;
import com.labs.dm.auto_tethering.activity.MainActivity;

import static com.labs.dm.auto_tethering.AppProperties.IDLE_3G_OFF_TIME;
import static com.labs.dm.auto_tethering.AppProperties.IDLE_TETHERING_OFF_TIME;

/**
 * Created by Daniel Mroczka on 10/7/2016.
 */
public class RegisterIdleListenerHelper extends AbstractRegisterHelper {

    public RegisterIdleListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        EditTextPreference tetheringIdleTime = getEditTextPreference(IDLE_TETHERING_OFF_TIME);
        tetheringIdleTime.setOnPreferenceChangeListener(changeListener);
        EditTextPreference internetIdleTime = getEditTextPreference(IDLE_3G_OFF_TIME);
        internetIdleTime.setOnPreferenceChangeListener(changeListener);
        final PreferenceScreen connectedClients = getPreferenceScreen("idle.connected.clients");

        connectedClients.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                connectedClients.setTitle("Connected clients: " + Utils.connectedClients());
                return false;
            }
        });

    }
}
