package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.labs.dm.auto_tethering.R;

/**
 * Created by daniel on 2015-10-12.
 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Auto WIFI Tethering ver. " + pInfo.versionName);
    }

}
