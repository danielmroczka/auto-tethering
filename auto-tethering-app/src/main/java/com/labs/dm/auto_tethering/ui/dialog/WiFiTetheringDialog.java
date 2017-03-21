package com.labs.dm.auto_tethering.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
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

    public WiFiTetheringDialog(Context context, WiFiTethering entity) {
        super(context, R.style.AppTheme);

        this.setContentView(R.layout.wifidialog);
        this.entity = entity;
        init();
    }

    private void init() {
        final EditText ssid = (EditText) findViewById(R.id.ssid);
        final EditText password = (EditText) findViewById(R.id.password);
        Spinner channels = (Spinner) findViewById(R.id.channel);
        Spinner types = (Spinner) findViewById(R.id.securityType);

        if (entity != null) {
            ssid.setText(entity.getSsid());
            password.setText(entity.getPassword());
        }

        Button btn = (Button) findViewById(R.id.saveBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(ssid.getText())) {
                    Toast.makeText(getContext(), "Fill all fields!", Toast.LENGTH_LONG).show();
                } else {
                    if (entity == null) {
                        entity = new WiFiTethering(ssid.getText().toString(), WiFiTethering.SECURITY_TYPE.OPEN, password.getText().toString(), 1, 1);
                    }
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

    public WiFiTethering getEntity() {
        return entity;
    }
}
