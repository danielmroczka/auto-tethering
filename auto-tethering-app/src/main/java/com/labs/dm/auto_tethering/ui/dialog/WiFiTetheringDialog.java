package com.labs.dm.auto_tethering.ui.dialog;

import android.app.Dialog;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.db.WiFiTethering;


/**
 * Created by Daniel Mroczka on 21-Mar-17.
 */

public class WiFiTetheringDialog extends Dialog {

    private WiFiTethering entity;

    public WiFiTetheringDialog(PreferenceActivity context, WiFiTethering entity) {
        super(context, R.style.AppTheme);

        this.setContentView(R.layout.wifidialog);
        this.entity = entity;
        init();
    }

    private void init() {
        final EditText ssid = (EditText) findViewById(R.id.ssid);
        final EditText password = (EditText) findViewById(R.id.password);
        final Spinner channels = (Spinner) findViewById(R.id.channel);
        final Spinner types = (Spinner) findViewById(R.id.securityType);
        final CheckBox defaultWifi = (CheckBox) findViewById(R.id.defaultWifi);

        if (entity != null) {
            ssid.setText(entity.getSsid());
            password.setText(entity.getPassword());
            defaultWifi.setChecked(entity.isDefaultWiFi());
        }

        Button btn = (Button) findViewById(R.id.saveBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validate()) {
                    Toast.makeText(getContext(), "Please fill all required fields!", Toast.LENGTH_LONG).show();
                } else {
                    if (entity == null) {
                        entity = new WiFiTethering(ssid.getText().toString(),
                                WiFiTethering.SECURITY_TYPE.valueOf((String) types.getSelectedItem()),
                                password.getText().toString(),
                                Integer.valueOf(channels.getSelectedItem().toString()),
                                0);
                    } else {
                        entity.setSsid(ssid.getText().toString());
                        entity.setPassword(password.getText().toString());
                        entity.setType(WiFiTethering.SECURITY_TYPE.valueOf((String) types.getSelectedItem()));
                        entity.setChannel(Integer.valueOf(channels.getSelectedItem().toString()));
                    }
                    entity.setDefaultWiFi(defaultWifi.isChecked());

                    dismiss();
                }
            }
        });
        Button closeBtn = (Button) findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
    }

    private boolean validate() {
        final EditText ssid = (EditText) findViewById(R.id.ssid);
        final EditText password = (EditText) findViewById(R.id.password);
        final Spinner types = (Spinner) findViewById(R.id.securityType);

        return !(TextUtils.isEmpty(ssid.getText()) || (!types.getSelectedItem().equals("OPEN") && (TextUtils.isEmpty(password.getText()) || password.getText().length() < 8)));
    }

    public WiFiTethering getEntity() {
        return entity;
    }
}
