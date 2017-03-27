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
import com.labs.dm.auto_tethering.db.WiFiTethering.SECURITY_TYPE;


/**
 * Created by Daniel Mroczka on 21-Mar-17.
 */

public class WiFiTetheringDialog extends Dialog {

    private WiFiTethering entity;

    public WiFiTetheringDialog(PreferenceActivity context, WiFiTethering entity) {
        super(context);
        this.setContentView(R.layout.wifi_dialog);
        this.entity = entity;
        initDialog();
    }

    private void initDialog() {
        final EditText ssid = (EditText) findViewById(R.id.ssid);
        final EditText password = (EditText) findViewById(R.id.password);
        final Spinner types = (Spinner) findViewById(R.id.securityType);
        final CheckBox defaultWifi = (CheckBox) findViewById(R.id.defaultWifi);
        final CheckBox hiddenWifi = (CheckBox) findViewById(R.id.hiddenWifi);

        initComponents(ssid, password, types, defaultWifi, hiddenWifi);
        setTitle(entity == null ? "New WiFi Hotspot" : "Modify WiFi Hotspot");

        final Button btn = (Button) findViewById(R.id.saveBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    if (entity == null) {
                        entity = new WiFiTethering();
                    }

                    entity.setSsid(ssid.getText().toString());
                    entity.setPassword(password.getText().toString());
                    entity.setType(SECURITY_TYPE.valueOf((String) types.getSelectedItem()));
                    entity.setHidden(hiddenWifi.isChecked());
                    entity.setDefaultWiFi(defaultWifi.isChecked());

                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Please fill all required fields!\nMinimal length of password is 8, maximal length is 63", Toast.LENGTH_LONG).show();
                }
            }
        });

        final Button closeBtn = (Button) findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
    }

    private void initComponents(EditText ssid, EditText password, Spinner types, CheckBox defaultWifi, CheckBox hiddenWifi) {
        if (entity != null) {
            ssid.setText(entity.getSsid());
            password.setText(entity.getPassword());
            defaultWifi.setChecked(entity.isDefaultWiFi());
            defaultWifi.setEnabled(!defaultWifi.isChecked());
            hiddenWifi.setChecked(entity.isHidden());
            String[] countryValue = getContext().getResources().getStringArray(R.array.securityTypes);
            for (int i = 0; i < countryValue.length; i++) {
                if (countryValue[i].equals(entity.getType().name())) {
                    types.setSelection(i, true);
                    break;
                }
            }
        }
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
