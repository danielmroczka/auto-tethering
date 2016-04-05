package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.DBManager;

public class ScheduleActivity extends Activity {

    private DBManager db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        db = DBManager.getInstance(getApplicationContext());

        Button cancel = (Button) findViewById(R.id.button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                moveTaskToBack(true);
            }
        });

        Button ok = (Button) findViewById(R.id.button2);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivityForResult(new Intent(ScheduleActivity.this, MainActivity.class), 123);
                //onBackPressed();

                insertSchedule();

                Intent intent = new Intent();
                intent.putExtra("edittextvalue", "value_here");
                setResult(RESULT_OK, intent);
                finish();//(ScheduleActivity.this, 123);
            }
        });
    }

    private void insertSchedule() {
        TimePicker time1 = (TimePicker) findViewById(R.id.timePicker);
        TimePicker time2 = (TimePicker) findViewById(R.id.timePicker2);
        ToggleButton[] dayweek = new ToggleButton[7];
        dayweek[0] = (ToggleButton) findViewById(R.id.btnMonday);
        dayweek[1] = (ToggleButton) findViewById(R.id.btnTuesday);
        dayweek[2] = (ToggleButton) findViewById(R.id.btnWednesday);
        dayweek[3] = (ToggleButton) findViewById(R.id.btnThursday);
        dayweek[4] = (ToggleButton) findViewById(R.id.btnFriday);
        dayweek[5] = (ToggleButton) findViewById(R.id.btnSaturday);
        dayweek[6] = (ToggleButton) findViewById(R.id.btnSunday);

        int mask = 0;
        for (int day = 0; day < 7; day++) {
            if (dayweek[day].isChecked()) {
                mask += Math.pow(2, day);
            }
        }

        Cron cron = new Cron(time1.getCurrentHour(), time1.getCurrentMinute(), time2.getCurrentHour(), time2.getCurrentMinute(), mask, Cron.STATUS.SCHED_OFF_ENABLED.getValue());
        db.addOrUpdateCron(cron);
    }
}
