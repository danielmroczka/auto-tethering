package com.labs.dm.auto_tethering;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class LogActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        show();

        ScrollView view = findViewById(R.id.scrollView);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                show();
                return false;
            }
        });

    }

    private void show() {
        final TextView textView = findViewById(R.id.textView);

        final ProgressDialog progress = new ProgressDialog(this, R.style.MyTheme);
        progress.setCancelable(false);
        progress.setIndeterminateDrawable(this.getResources().getDrawable(R.drawable.progress));
        progress.show();
        final StringBuilder log = new StringBuilder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //try {
                log.append(MyLog.getContent(MyLog.LEVEL.debug));
//                    Process process = Runtime.getRuntime().exec("logcat -d TetheringService:D *:S");
//                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                    String line;
//                    while ((line = bufferedReader.readLine()) != null) {
//                        log.append(line).append("\n");
//                    }
                // } catch (IOException e) {

                // }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(log.toString());
                        progress.dismiss();
                    }
                });
            }
        }).start();
    }
}