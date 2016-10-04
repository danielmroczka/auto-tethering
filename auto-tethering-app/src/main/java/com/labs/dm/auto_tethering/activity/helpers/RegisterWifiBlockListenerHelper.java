package com.labs.dm.auto_tethering.activity.helpers;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import com.labs.dm.auto_tethering.activity.MainActivity;

/**
 * Created by Daniel Mroczka on 10/4/2016.
 */
public class RegisterWifiBlockListenerHelper extends AbstractRegisterHelper {

    protected RegisterWifiBlockListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        CheckBoxPreference block = getCheckBoxPreference("wifi.connected.block.tethering");
        block.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return false;
            }
        });
    }
}
