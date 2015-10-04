package com.labs.dm.auto_tethering;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class MainActivity extends Activity {

    private Button btn;
    private Button readButton;
    private CheckBox activateOnStartup;
    private CheckBox activate3G;
    private CheckBox activateTethering;
    private CheckBox scheduler;
    private CheckBox activateOnPhoneNumber;

    private EditText switchOffTime;
    private EditText switchOnTime;
    private EditText phoneNumber;

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
        activateOnPhoneNumber = (CheckBox) findViewById(R.id.onlyOnPhoneNumberCheckBox);

        switchOffTime = (EditText) findViewById(R.id.switchOffTime);
        switchOnTime = (EditText) findViewById(R.id.switchOnTime);
        phoneNumber = (EditText) findViewById(R.id.editText2);

        btn = (Button) findViewById(R.id.button);
        readButton = (Button) findViewById(R.id.readButton);

        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TelephonyManager tMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                String mPhoneNumber = tMgr.getLine1Number();
                phoneNumber.setText(mPhoneNumber);
            }
        });

        activateOnPhoneNumber.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                phoneNumber.setEnabled(isChecked);
            }
        });

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
