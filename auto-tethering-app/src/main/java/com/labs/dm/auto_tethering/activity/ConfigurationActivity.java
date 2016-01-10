package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.receiver.TetheringWidgetProvider;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

/**
 * Created by daniel on 2016-01-09.
 */
public class ConfigurationActivity extends Activity {
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
    }

    private void handleOkButton() {
        showAppWidget();
    }

    int mAppWidgetId;

    private void showAppWidget() {

        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);

            Intent startService = new Intent(ConfigurationActivity.this, TetheringWidgetProvider.class);
            startService.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
            startService.setAction("FROM CONFIGURATION ACTIVITY");
            setResult(RESULT_OK, startService);
            startService(startService);
            finish();
        }

        if (mAppWidgetId == INVALID_APPWIDGET_ID) {
            Log.e("Invalid app widget id", "Invalid app widget id");
            finish();
        }

    }
}
