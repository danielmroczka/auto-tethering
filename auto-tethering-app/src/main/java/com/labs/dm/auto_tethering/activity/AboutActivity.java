package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.labs.dm.auto_tethering.BuildConfig;
import com.labs.dm.auto_tethering.MyLog;
import com.labs.dm.auto_tethering.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
            MyLog.e("About", e);
        }

        TextView textView = findViewById(R.id.versionTextView);
        String buildTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(BuildConfig.buildTime);
        textView.setText(String.format("version: %s.%s\n", pInfo != null ? pInfo.versionName : null, BuildConfig.BUILD_TYPE.toUpperCase()));
        textView.append(String.format("build: %s", buildTime));
    }

}
