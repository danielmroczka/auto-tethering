package com.labs.dm.auto_tethering.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;

/**
 * Created by daniel on 2017-03-27.
 */

public class ReleaseDialog extends Dialog {

    public ReleaseDialog(Context context) {
        super(context);
        this.setContentView(R.layout.release_dialog);
        initDialog();
    }

    private void initDialog() {
        Button closeBtn = (Button) findViewById(R.id.releaseCloseBtn);
        ListView listView = (ListView) findViewById(R.id.list);
        setTitle("Auto WiFi Tethering " + BuildConfig.VERSION_NAME);
        String[] countryValue = getContext().getResources().getStringArray(R.array.current);
        ListAdapter adapter = new ArrayAdapter<>(getContext(), R.layout.release_item, countryValue);
        listView.setAdapter(adapter);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}
