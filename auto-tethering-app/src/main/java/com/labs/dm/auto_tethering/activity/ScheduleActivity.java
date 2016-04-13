package com.labs.dm.auto_tethering.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.labs.dm.auto_tethering.R;
import com.labs.dm.auto_tethering.db.Cron;
import com.labs.dm.auto_tethering.db.DBManager;

public class ScheduleActivity extends Activity {
    private int[] buttons = {R.id.btnMonday, R.id.btnTuesday, R.id.btnWednesday, R.id.btnThursday, R.id.btnFriday, R.id.btnSaturday, R.id.btnSunday};
    private DBManager db;
    private TimePicker timeOff;
    private TimePicker timeOn;
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        db = DBManager.getInstance(getApplicationContext());
        timeOff = (TimePicker) findViewById(R.id.scheduleTimeOff);
        timeOn = (TimePicker) findViewById(R.id.scheduleTimeOn);
        timeOff.setIs24HourView(DateFormat.is24HourFormat(this));
        timeOn.setIs24HourView(DateFormat.is24HourFormat(this));
        readData();

        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        Button btnOk = (Button) findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (insertSchedule()) {
                    setResult(RESULT_OK, new Intent());
                    finish();
                }
            }
        });
    }

    private void readData() {
        Intent intent = getIntent();
        if (intent.getIntExtra("cronId", 0) > 0) {
            Cron cron = db.getCron(intent.getIntExtra("cronId", 0));
            id = cron.getId();
            timeOff.setCurrentHour(cron.getHourOff());
            timeOn.setCurrentHour(cron.getHourOn());
            timeOff.setCurrentMinute(cron.getMinOff());
            timeOn.setCurrentMinute(cron.getMinOn());
            String binary = String.format("%7s", Integer.toBinaryString(cron.getMask())).replace(' ', '0');
            for (int day = 0; day < buttons.length; day++) {
                ToggleButton button = (ToggleButton) findViewById(buttons[6 - day]);
                button.setChecked(binary.substring(day, day + 1).equals("1"));
            }
        }
    }

    private boolean insertSchedule() {
        ToggleButton[] daysOfWeek = new ToggleButton[7];
        int mask = 0;
        for (int day = 0; day < 7; day++) {
            daysOfWeek[day] = (ToggleButton) findViewById(buttons[day]);
            if (daysOfWeek[day].isChecked()) {
                mask += Math.pow(2, day);
            }
        }

        if (mask == 0) {
            Toast.makeText(getApplicationContext(), "You need to select at least one day!", Toast.LENGTH_LONG).show();
            return false;
        }

        Cron cron = new Cron(timeOff.getCurrentHour(), timeOff.getCurrentMinute(), timeOn.getCurrentHour(), timeOn.getCurrentMinute(), mask, Cron.STATUS.SCHED_OFF_ENABLED.getValue());
        cron.setId(id);
        if (db.addOrUpdateCron(cron) <= 0) {
            Toast.makeText(getApplicationContext(), "Cannot add the same schedule items more than once!", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}
