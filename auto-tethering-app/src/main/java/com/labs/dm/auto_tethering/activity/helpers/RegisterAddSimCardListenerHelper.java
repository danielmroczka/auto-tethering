package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.SimCard;

import java.util.List;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by Daniel Mroczka on 2016-09-13.
 */

public class RegisterAddSimCardListenerHelper extends AbstractRegisterHelper {

    public RegisterAddSimCardListenerHelper(MainActivity activity) {
        super(activity);
    }

    @Override
    public void registerUIListeners() {
        final TelephonyManager tMgr = (TelephonyManager) activity.getSystemService(TELEPHONY_SERVICE);
        final String ssn = tMgr.getSimSerialNumber();
        boolean status = db.isOnWhiteList(ssn);

        PreferenceScreen addSimCard = (PreferenceScreen) activity.findPreference("add.current.simcard");
        addSimCard.setEnabled(!status);
        final String[] number = {""};
        addSimCard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                number[0] = tMgr.getLine1Number();
                // TODO:
                if (number[0] == null || number[0].isEmpty()) {
                    LayoutInflater li = LayoutInflater.from(activity);
                    final View promptsView = li.inflate(R.layout.add_simcard_prompt, null);
                    new AlertDialog.Builder(activity)
                            .setTitle("Add phone number")
                            .setMessage("Cannot retrieve telephone number. Please provide it manually")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setView(promptsView)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
                                    number[0] = userInput.getText().toString();
                                    addSimCard(number[0]);
                                }
                            })
                            .setNegativeButton(R.string.no, null).show();
                    return true;
                } else {
                    addSimCard(number[0]);
                }
                return true;
            }
        });

        final PreferenceScreen removeSimCard = (PreferenceScreen) activity.findPreference("remove.simcard");
        removeSimCard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PreferenceCategory pc = getPreferenceCategory("simcard.list");
                for (int idx = pc.getPreferenceCount() - 1; idx >= 0; idx--) {
                    Preference pref = pc.getPreference(idx);
                    if (pref instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) pref).isChecked();
                        if (status) {
                            String ssn = pref.getKey().toString();
                            if (db.removeSimCard(ssn) > 0) {
                                pc.removePreference(pref);
                                removeSimCard.setEnabled(pc.getPreferenceCount() > 2);
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    private void addSimCard(String number) {
        final TelephonyManager tMgr = (TelephonyManager) activity.getSystemService(TELEPHONY_SERVICE);
        final String ssn = tMgr.getSimSerialNumber();

        boolean isAlreadyAdded = db.isOnWhiteList(ssn);
        if (isAlreadyAdded) {
            Toast.makeText(activity, "SimCard " + number + " is already added on the whitelist", Toast.LENGTH_LONG).show();
            return;
        }

        SimCard simcard = new SimCard(tMgr.getSimSerialNumber(), number, 0);
        if (db.addSimCard(simcard) > 0) {
            prepare();
        }
    }

    @Override
    public void prepare() {
        PreferenceCategory pc = getPreferenceCategory("simcard.list");
        List<SimCard> list = db.readSimCard();
        for (int idx = 0; idx < pc.getPreferenceCount(); idx++) {
            Object object = pc.getPreference(idx);
            if (object instanceof CheckBoxPreference) {
                pc.removePreference((CheckBoxPreference) object);
            }
        }
        for (SimCard item : list) {
            Preference ps = new CheckBoxPreference(activity);
            ps.setPersistent(false);
            ps.setTitle(item.getNumber());
            ps.setKey(item.getSsn());
            pc.addPreference(ps);
        }

        PreferenceScreen ps = getPreferenceScreen("add.current.simcard");
        activity.findPreference("remove.simcard").setEnabled(pc.getPreferenceCount() > 2);
        ps.setEnabled(true);
    }
}
