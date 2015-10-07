package com.labs.dm.auto_tethering.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.labs.dm.auto_tethering.AppProperties;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.service.TetheringService;

public class MainActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this, TetheringService.class);
        startService(serviceIntent);
        WifiConfiguration cfg = TetheringService.getWifiApConfiguration(getApplicationContext());
        prefs.edit().putString(AppProperties.SSID, cfg.SSID).apply();
        displayPrompt();
    }

    private void displayPrompt() {
        if (!prefs.getString(AppProperties.LATEST_VERSION, "").isEmpty()) {
            return;
        } else {
            prefs.edit().putString(AppProperties.LATEST_VERSION, "1").apply();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Warning");
        builder.setMessage("By default connection to internet and WIFI tethering are turned off. " +
                "You need to set it manually. " +
                "\nConnecting via packet data may incur additional charges. " +
                "\nWIFI tethering will increase battery consumption. " +
                "\n\nDo you want to turn on 3G connection and WIFI tethering?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean(AppProperties.ACTIVATE_3G, true).apply();
                prefs.edit().putBoolean(AppProperties.ACTIVATE_TETHERING, true).apply();
            }

        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case AppProperties.ACTIVATE_ON_SIMCARD: {
                if (sharedPreferences.getBoolean(AppProperties.ACTIVATE_ON_SIMCARD, false)) {
                    TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    String simCard = tMgr.getSimSerialNumber();
                    if (simCard == null || simCard.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Unable to retrieve SIM Card Serial!", Toast.LENGTH_LONG).show();
                        prefs.edit().putBoolean(AppProperties.ACTIVATE_ON_SIMCARD, false).apply();
                        prefs.edit().putString(AppProperties.SIMCARD, "").apply();
                    } else {
                        prefs.edit().putString(AppProperties.SIMCARD, simCard);
                    }
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        System.out.println(preference);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

}
