package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.R;

import java.text.SimpleDateFormat;

/**
 * Created by Daniel Mroczka
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

        TextView textView = (TextView) findViewById(R.id.versionTextView);
        String buildTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(BuildConfig.buildTime);
        textView.setText(String.format("version: %s\n", pInfo != null ? pInfo.versionName : null));
        textView.append(String.format("build: %s", buildTime));
    }

}
