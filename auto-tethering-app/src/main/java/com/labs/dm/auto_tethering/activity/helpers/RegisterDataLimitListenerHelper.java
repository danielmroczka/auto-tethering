package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.TetherIntents;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.service.ServiceHelper;

/**
 * Created by Daniel Mroczka on 2016-09-28.
 */

public class RegisterDataLimitListenerHelper extends AbstractRegisterHelper {

    public RegisterDataLimitListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        getPreferenceScreen("data.limit.reset").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.warning)
                        .setMessage("Do you want to reset data usage counter?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                long dataUsage = ServiceHelper.getDataUsage();
                                prefs.edit().putLong("data.usage.reset.value", dataUsage).apply();
                                prefs.edit().putLong("data.usage.last.value", dataUsage).apply();
                                //prefs.edit().putLong("data.usage.reset.timestamp", System.currentTimeMillis()).apply();
                                prefs.edit().putLong("data.usage.removeAllData.timestamp", System.currentTimeMillis()).apply();
                                Intent intent = new Intent(TetherIntents.DATA_USAGE);
                                activity.sendBroadcast(intent);
                            }
                        })
                        .setNegativeButton(R.string.no, null
                        ).show();

                return true;
            }
        });

        getEditTextPreference("data.limit.value").setOnPreferenceChangeListener(changeListener);
    }
}
