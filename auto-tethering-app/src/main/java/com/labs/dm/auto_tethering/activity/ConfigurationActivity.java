package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.receiver.TetheringWidgetProvider;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

/**
 * Created by Daniel Mroczka on 2016-01-09.
 */
public class ConfigurationActivity extends Activity {

    private int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        setResult(RESULT_CANCELED);
        initListViews();
    }

    public void initListViews() {
        Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handleOkButton();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CheckBox mobile = (CheckBox) findViewById(R.id.chkWidget3G);
            mobile.setChecked(false);
            mobile.setEnabled(false);
        }
    }

    private void handleOkButton() {
        showAppWidget();
    }

    private void showAppWidget() {

        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);

            if (mAppWidgetId == INVALID_APPWIDGET_ID) {
                Log.w("WidgetAdd", "Cannot continue. Widget ID incorrect");
                return;
            }

            CheckBox mobile = (CheckBox) findViewById(R.id.chkWidget3G);
            CheckBox tethering = (CheckBox) findViewById(R.id.chkWidgetWifi);
            CheckBox startService = (CheckBox) findViewById(R.id.chkStartService);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.edit().putBoolean(key("mobile"), mobile.isChecked()).apply();
            prefs.edit().putBoolean(key("tethering"), tethering.isChecked()).apply();
            prefs.edit().putBoolean(key("start.service"), startService.isChecked()).apply();

            Intent serviceIntent = new Intent(ConfigurationActivity.this, TetheringWidgetProvider.class);
            serviceIntent.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, serviceIntent);
            startService(serviceIntent);
            finish();
        }

        if (mAppWidgetId == INVALID_APPWIDGET_ID) {
            Log.e("WidgetAdd", "Invalid app widget id");
            finish();
        }

    }

    private String key(String key) {
        return "widget." + mAppWidgetId + "." + key;
    }
}
