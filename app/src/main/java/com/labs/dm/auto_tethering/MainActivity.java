package com.labs.dm.auto_tethering;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {


    private CheckBox activateOnStartup;
    private CheckBox activate3G;
    private CheckBox activateTethering;
    private CheckBox scheduler;
    private CheckBox activeOnSIMCard;

    private EditText switchOffTime;
    private EditText switchOnTime;


    private AppProperties props;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        props = new AppProperties();
        props.load(getBaseContext());
        activateOnStartup = (CheckBox) findViewById(R.id.activateOnStartupCheckBox);
        activate3G = (CheckBox) findViewById(R.id.turnOn3GCheckBox);
        activateTethering = (CheckBox) findViewById(R.id.turnOnWifiCheckBox);
        scheduler = (CheckBox) findViewById(R.id.schedulerCheckBox);
        activeOnSIMCard = (CheckBox) findViewById(R.id.onlyOnPhoneNumberCheckBox);

        switchOffTime = (EditText) findViewById(R.id.switchOffTime);
        switchOnTime = (EditText) findViewById(R.id.switchOnTime);

        activeOnSIMCard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                String simCard = tMgr.getSimSerialNumber();
                if (simCard == null || simCard.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Unable to retrieve SIM Card Serial!", Toast.LENGTH_LONG).show();
                    activeOnSIMCard.setChecked(false);
                }

                props.setSimCard(simCard);
            }
        });

        activeOnSIMCard.setChecked(props.getSimCard() != null || !props.getSimCard().isEmpty());

        scheduler.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switchOffTime.setEnabled(isChecked);
                switchOnTime.setEnabled(isChecked);
            }
        });

        activateOnStartup.setChecked(props.isActivateOnStartup());
        activate3G.setChecked(props.isActivate3G());
        activateTethering.setChecked(props.isActivateTethering());
        switchOnTime.setText(props.getTimeOn());
        switchOffTime.setText(props.getTimeOff());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this, TetheringService.class);
        startService(serviceIntent);
        displayPrompt();
    }

    private void displayPrompt() {
        if (props.getLatestVersion() != null || !props.getLatestVersion().isEmpty()) {
            return;
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
                activate3G.setChecked(true);
                activateTethering.setChecked(true);
                persist();
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

    private void persist() {
        props.setActivate3G(activate3G.isChecked());
        props.setActivateTethering(activateTethering.isChecked());
        props.setActivateOnStartup(activateOnStartup.isChecked());
        props.setScheduler(scheduler.isChecked());
        props.setTimeOff(switchOffTime.getText().toString());
        props.setTimeOn(switchOnTime.getText().toString());
        props.save(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        persist();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persist();
    }
}
