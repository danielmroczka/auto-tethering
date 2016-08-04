package com.labs.dm.auto_tethering;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        show();
    }

    private void show() {
        final TextView tv = (TextView) findViewById(R.id.textView1);
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();
        final StringBuilder log = new StringBuilder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process process = Runtime.getRuntime().exec("logcat -d TetheringService:D *:S");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        log.append(line).append("\n");
                    }
                } catch (IOException e) {

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(log.toString());
                        progress.dismiss();
                    }
                });
            }
        }).start();
    }
}