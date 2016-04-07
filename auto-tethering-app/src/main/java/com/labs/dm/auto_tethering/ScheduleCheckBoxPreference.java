package com.labs.dm.auto_tethering;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.labs.dm.auto_tethering.db.Cron;

/**
 * Created by Daniel Mroczka on 2016-04-04.
 */
public class ScheduleCheckBoxPreference extends CheckBoxPreference {
    private ImageButton button;
    private Cron cron;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public ScheduleCheckBoxPreference(Cron cron, Context context) {
        super(context);
        this.cron = cron;
        //ImageButton btnToogle = (ImageButton) context.get.this.findViewById(R.id.btnToggle);
    }

    @Override
    protected void onBindView(View view) {

        super.onBindView(view);
        final ImageButton btnToogle = (ImageButton) view.findViewById(R.id.btnToggle);
        final CheckBox chk = (CheckBox) view.findViewById(R.id.chk);

        chk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setChecked(chk.isChecked());
            }
        });

        btnToogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cron.getStatus() == Cron.STATUS.SCHED_OFF_ENABLED.getValue()) {
                    cron.setStatus(Cron.STATUS.SCHED_OFF_DISABLED.getValue());
                    // chk.setChecked(false);

                } else if (cron.getStatus() == Cron.STATUS.SCHED_ON_DISABLED.getValue()) {
                    cron.setStatus(Cron.STATUS.SCHED_OFF_ENABLED.getValue());
                    //  chk.setChecked(true);

                }
            }
        });
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.add_schedule_item, parent, false);
        //return super.onCreateView(parent);

    }

  /*  View.OnClickListener imgButtonHandler = new View.OnClickListener() {

        public void onClick(View v) {
            button.setBackgroundResource(R.drawable.ic_launcher);

        }
    };*/
}
