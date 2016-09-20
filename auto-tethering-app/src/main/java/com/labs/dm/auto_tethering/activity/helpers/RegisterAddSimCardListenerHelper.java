package com.labs.dm.auto_tethering.activity.helpers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.activity.MainActivity;
import com.labs.dm.auto_tethering.db.DBManager;
import com.labs.dm.auto_tethering.db.SimCard;

import java.util.List;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by Daniel Mroczka on 2016-09-13.
 */

public class RegisterAddSimCardListenerHelper {
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final DBManager db;

    public RegisterAddSimCardListenerHelper(MainActivity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
        this.db = DBManager.getInstance(activity);
    }

    public void registerAddSimCardListener() {
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
                PreferenceCategory pc = (PreferenceCategory) activity.findPreference("simcard.list");
                for (int idx = pc.getPreferenceCount() - 1; idx >= 0; idx--) {
                    Preference pref = pc.getPreference(idx);
                    if (pref instanceof CheckBoxPreference) {
                        boolean status = ((CheckBoxPreference) pref).isChecked();
                        if (status) {
                            pc.removePreference(pref);
                            db.removeSimCard((String) pref.getTitle());
                            removeSimCard.setEnabled(pc.getPreferenceCount() > 2);
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
        SimCard simcard = new SimCard(tMgr.getSimSerialNumber(), number, 0);
        db.addSimCard(simcard);
        boolean status = db.isOnWhiteList(ssn);
        PreferenceScreen p = (PreferenceScreen) activity.findPreference("add.current.simcard");
        p.setEnabled(!status);
        prepareSimCardWhiteList();
    }

    public void prepareSimCardWhiteList() {
        PreferenceCategory pc = (PreferenceCategory) activity.findPreference("simcard.list");
        List<SimCard> list = db.readSimCard();
        for (int idx = 0; idx < pc.getPreferenceCount(); idx++) {
            Object object = pc.getPreference(idx);
            if (object instanceof CheckBoxPreference) {
                pc.removePreference((CheckBoxPreference) object);
            }
        }
        for (SimCard item : list) {
            Preference ps = new CheckBoxPreference(activity);
            ps.setTitle(item.getNumber());
            ps.setSummary("SSN: " + item.getSsn());
            pc.addPreference(ps);
        }

        PreferenceScreen ps = (PreferenceScreen) activity.findPreference("add.current.simcard");
        activity.findPreference("remove.simcard").setEnabled(pc.getPreferenceCount() > 2);
        ps.setEnabled(true);
    }
}
